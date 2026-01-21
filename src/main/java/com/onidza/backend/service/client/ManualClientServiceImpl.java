package com.onidza.backend.service.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.config.CacheKeys;
import com.onidza.backend.config.CacheTtlProps;
import com.onidza.backend.config.CacheVersionService;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.service.TransactionAfterCommitExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@AllArgsConstructor
@Service
public class ManualClientServiceImpl implements ClientService {

    private final MapperService mapperService;
    private final ClientRepository clientRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private final CacheTtlProps ttlProps;
    private final CacheVersionService versionService;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    private static final String CLIENT_NOT_FOUND = "Client not found";

    @Override
    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long id) {
        log.info("Service called getClientById with id: {}", id);

        String objFromCache = stringRedisTemplate.opsForValue().get(CacheKeys.CLIENT_KEY_PREFIX+ id);

        try {
            if (objFromCache != null) {
                log.info("Returned client from cache with id: {}", id);
                return objectMapper.readValue(objFromCache, ClientDTO.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to read client from cache with key {}", CacheKeys.CLIENT_KEY_PREFIX + id, e);
        }

        Client clientFromDb = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        try {
            stringRedisTemplate.opsForValue().set(
                    CacheKeys.CLIENT_KEY_PREFIX + id,
                    objectMapper.writeValueAsString(mapperService.clientToDTO(clientFromDb)),
                    ttlProps.clientById()
            );
            log.info("getClientById was cached...");

        } catch (JsonProcessingException e) {
            log.warn("Failed to write client to cache with key {}", CacheKeys.CLIENT_KEY_PREFIX + id, e);
        }

        log.info("Returned client from db with id: {}", id);
        return mapperService.clientToDTO(clientFromDb);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientsPageDTO getClientsPage(int page, int size) {
        log.info("Service called getClientsPage");

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);

        long ver = versionService.getKeyVersion(CacheKeys.CLIENTS_PAGE_VER_KEY);
        String key = CacheKeys.CLIENTS_PAGE_VER_KEY + ver + ":p=" + safePage + ":s=" + safeSize;

        Object objFromCache = redisTemplate.opsForValue().get(key);
        if (objFromCache != null) {
            ClientsPageDTO cached = objectMapper.convertValue(objFromCache, ClientsPageDTO.class);

            log.info("Returned page from cache with size={}", cached.items().size());
            return cached;
        }

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("id").ascending());

        Page<ClientDTO> result = clientRepository.findAll(pageable)
                .map(mapperService::clientToDTO);

        ClientsPageDTO response = new ClientsPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        redisTemplate.opsForValue().set(key, response, ttlProps.getClientsPage());
        log.info("getClientsPage was cached...");

        log.info("Returned page from db with size={}", response.items().size());
        return response;
    }

    @Override
    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        log.info("Service called addClient");

        Client client = mapperService.clientDTOToEntity(clientDTO);

        if (client.getProfile() != null) {
            client.getProfile().setClient(client);
        }

        Client saved = clientRepository.save(client);

        boolean hasCoupons = client.getCoupons() != null && !client.getCoupons().isEmpty();
        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheKeys.CLIENTS_PAGE_VER_KEY);
            log.info("Key {} was incremented", CacheKeys.CLIENTS_PAGE_VER_KEY);

            if (hasCoupons) {
                versionService.bumpVersion(CacheKeys.COUPON_PAGE_VER_KEY);
                versionService.bumpVersion(CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

                log.info("Keys: {}, {} was incremented.",
                        CacheKeys.COUPON_PAGE_VER_KEY,
                        CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY
                );
            }
        });

        return mapperService.clientToDTO(saved);
    }

    @Override
    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        log.info("Service called updateClient with id: {}", id);

        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        existing.setName(clientDTO.name());
        existing.setEmail(clientDTO.email());

        Profile existingProfile = existing.getProfile();
        if (existing.getProfile() != null && clientDTO.profile() != null) {
            existingProfile.setAddress(clientDTO.profile().address());
            existingProfile.setPhone(clientDTO.profile().phone());
        }

        if (clientDTO.coupons() != null) {
            existing.getCoupons().clear();
            clientDTO.coupons()
                    .stream()
                    .map(mapperService::couponDTOToEntity)
                    .forEach(coupon -> {
                        existing.getCoupons().add(coupon);
                        coupon.getClients().add(existing);
                    });
        }

        if (clientDTO.orders() != null) {
            existing.getOrders().clear();
            clientDTO.orders()
                    .stream()
                    .map(mapperService::orderDTOToEntity)
                    .forEach(order -> {
                        existing.getOrders().add(order);
                        order.setClient(existing);
                    });
        }

        ClientDTO updated = mapperService.clientToDTO(clientRepository.save(existing));

        boolean couponsTouched = clientDTO.coupons() != null;
        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CacheKeys.CLIENT_KEY_PREFIX + id);
            versionService.bumpVersion(CacheKeys.CLIENTS_PAGE_VER_KEY);

            log.info("Key {} was invalidated. Key {} was incremented.",
                    CacheKeys.CLIENT_KEY_PREFIX + id,
                    CacheKeys.CLIENTS_PAGE_VER_KEY
            );

            if (couponsTouched) {
                versionService.bumpVersion(CacheKeys.COUPON_PAGE_VER_KEY);
                versionService.bumpVersion(CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

                log.info("Keys: {}, {} was incremented.",
                        CacheKeys.COUPON_PAGE_VER_KEY,
                        CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY
                );
            }
        });

        return updated;
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        log.info("Service called deleteClient with id: {}", id);

        if (!clientRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND);
        }

        clientRepository.deleteById(id);

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CacheKeys.CLIENT_KEY_PREFIX + id);
            versionService.bumpVersion(CacheKeys.CLIENTS_PAGE_VER_KEY);

            versionService.bumpVersion(CacheKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated",
                    CacheKeys.CLIENT_KEY_PREFIX + id,
                    CacheKeys.CLIENTS_PAGE_VER_KEY,

                    CacheKeys.COUPON_PAGE_VER_KEY,
                    CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY
            );
        });
    }
}
