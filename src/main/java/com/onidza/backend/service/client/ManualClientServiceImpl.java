package com.onidza.backend.service.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
@Service
public class ManualClientServiceImpl implements ClientService {

    private final MapperService mapperService;
    private final ClientRepository clientRepository;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final TransactionAfterCommitExecutor afterCommitExecutor;

    private static final String CLIENT_KEY_PREFIX = "client:";
    private static final long CLIENT_TTL_MINUTES = 1;

    private static final String PAGE_CLIENTS_KEY = "clients:";
    private static final Duration PAGE_CLIENTS_TTL = Duration.ofMinutes(1);

    private static final String ALL_COUPONS_KEY = "coupons:all:v1";
    private static final String ALL_COUPONS_BY_CLIENT_ID_KEY_PREFIX = "coupons:byClientId:v1:";

    //this one made by stringRedisTemplate
    @Override
    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long id) {
        log.info("Called getClientById with id: {}", id);

        String objFromCache = stringRedisTemplate.opsForValue().get(CLIENT_KEY_PREFIX + id);

        try {
            if (objFromCache != null) {
                log.info("Returned client from cash with id: {}", id);
                return objectMapper.readValue(objFromCache, ClientDTO.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to read client from cache for key {}", CLIENT_KEY_PREFIX + id, e);
        }

        Client clientFromDb = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        try {
            stringRedisTemplate.opsForValue().set(
                    CLIENT_KEY_PREFIX + id,
                    objectMapper.writeValueAsString(mapperService.clientToDTO(clientFromDb)),
                    CLIENT_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
            log.info("getClientById was cached...");

        } catch (JsonProcessingException e) {
            log.warn("Failed to write client to cache for key {}", CLIENT_KEY_PREFIX + id, e);
        }

        log.info("Returned client from db with id: {}", id);
        return mapperService.clientToDTO(clientFromDb);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientsPageDTO getClientsPage(int page, int size) {
        log.info("Called getAllClients");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        String key = PAGE_CLIENTS_KEY + ":p=" + safePage + ":s=" + safeSize;

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

        redisTemplate.opsForValue().set(key, response, PAGE_CLIENTS_TTL);
        log.info("getAllClients was cached...");

        log.info("Returned page from db with size={}", response.items().size());
        return response;
    }

    @Override
    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        log.info("Called addClient with name: {}", clientDTO.name());

        Client client = mapperService.clientDTOToEntity(clientDTO);

        if (client.getProfile() != null) {
            client.getProfile().setClient(client);
        }

        Client saved = clientRepository.save(client);

        boolean hasCoupons = client.getCoupons() != null && !client.getCoupons().isEmpty();
        afterCommitExecutor.run(() -> {
            redisTemplate.delete(PAGE_CLIENTS_KEY);
            log.info("Added a new client, getAllList was invalidated too with key={}", PAGE_CLIENTS_KEY);

            if (hasCoupons) {
                redisTemplate.delete(ALL_COUPONS_KEY);
                log.info("Added a new client, getAllCoupons was invalidated too with key={}", PAGE_CLIENTS_KEY);
            }
        });

        return mapperService.clientToDTO(saved);
    }

    @Override
    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        log.info("Called updateClient with id: {}", id);

        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

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
            redisTemplate.delete(CLIENT_KEY_PREFIX + id);
            redisTemplate.delete(PAGE_CLIENTS_KEY);

            log.info("Updated client was invalidated in cache with key={}", CLIENT_KEY_PREFIX + id);
            log.info("Updated client in getAllList was invalidated too with key={}", PAGE_CLIENTS_KEY);

            if (couponsTouched) {
                redisTemplate.delete(ALL_COUPONS_KEY);
                redisTemplate.delete(ALL_COUPONS_BY_CLIENT_ID_KEY_PREFIX + id);

                log.info("Updated client, getAllCoupons was invalidated too with key={}", ALL_COUPONS_KEY);
                log.info("Updated client, getAllCouponsByClientId was invalidated too with key={}",
                        ALL_COUPONS_BY_CLIENT_ID_KEY_PREFIX + id);
            }
        });

        return updated;
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        log.info("Called deleteClient with id: {}", id);

        if (!clientRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }

        clientRepository.deleteById(id);

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CLIENT_KEY_PREFIX + id);
            redisTemplate.delete(PAGE_CLIENTS_KEY);

            redisTemplate.delete(ALL_COUPONS_BY_CLIENT_ID_KEY_PREFIX + id);

            log.info("Deleted client was invalidated in cache with key={}", CLIENT_KEY_PREFIX + id);
            log.info("Deleted client in getAllList was invalidated too with key={}", PAGE_CLIENTS_KEY);
            log.info("Deleted client was invalidated in getAllCouponsByClientId with key={}", ALL_COUPONS_BY_CLIENT_ID_KEY_PREFIX + id);
        });
    }
}
