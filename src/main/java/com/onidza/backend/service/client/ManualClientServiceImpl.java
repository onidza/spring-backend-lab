package com.onidza.backend.service.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.config.cache.manual.CacheManualKeys;
import com.onidza.backend.config.cache.manual.CacheManualVersionKeys;
import com.onidza.backend.config.cache.manual.CacheTtlProps;
import com.onidza.backend.config.cache.CacheVersionService;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Coupon;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.service.TransactionAfterCommitExecutor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

import java.util.List;

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

    private final ApplicationEventPublisher publisher;

    private static final String CLIENT_NOT_FOUND = "Client not found";

    @Override
    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long id) {
        log.info("Service called getClientById with id: {}", id);

        String objFromCache = stringRedisTemplate.opsForValue().get(CacheManualKeys.CLIENT_KEY_PREFIX + id);

        try {
            if (objFromCache != null) {
                log.info("Returned client from cache with id: {}", id);
                return objectMapper.readValue(objFromCache, ClientDTO.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to read client from cache with key {}", CacheManualKeys.CLIENT_KEY_PREFIX + id, e);
        }

        Client clientFromDb = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        try {
            stringRedisTemplate.opsForValue().set(
                    CacheManualKeys.CLIENT_KEY_PREFIX + id,
                    objectMapper.writeValueAsString(mapperService.clientToDTO(clientFromDb)),
                    ttlProps.clientById()
            );
            log.info("getClientById was cached...");

        } catch (JsonProcessingException e) {
            log.warn("Failed to write client to cache with key {}", CacheManualKeys.CLIENT_KEY_PREFIX + id, e);
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

        long ver = versionService.getKeyVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);
        String key = CacheManualKeys.CLIENTS_PAGE_PREFIX + ver + ":p=" + safePage + ":s=" + safeSize;

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
        boolean hasOrders = client.getOrders() != null && !client.getOrders().isEmpty();

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.PROFILES_PAGE_VER_KEY);

            log.info("Keys: {}, {} was incremented",
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheManualVersionKeys.PROFILES_PAGE_VER_KEY
            );

            if (hasCoupons) {
                versionService.bumpVersion(CacheManualVersionKeys.COUPON_PAGE_VER_KEY);
                versionService.bumpVersion(CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

                log.info("Keys: {}, {} was incremented.",
                        CacheManualVersionKeys.COUPON_PAGE_VER_KEY,
                        CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY
                );
            }

            if (hasOrders) {
                versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_VER_KEY);
                versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
                versionService.bumpVersion(CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

                log.info("Keys: {}, {}, {} was incremented.",
                        CacheManualVersionKeys.ORDERS_PAGE_VER_KEY,
                        CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                        CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER
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

        ClientDTO saved = mapperService.clientToDTO(clientRepository.save(existing));

        List<Long> orderIds = existing.getOrders().stream().map(Order::getId).toList();
        List<Long> couponIds = existing.getCoupons().stream().map(Coupon::getId).toList();

        boolean profileTouched = clientDTO.profile() != null;
        boolean couponsTouched = clientDTO.coupons() != null;
        boolean ordersTouched = clientDTO.orders() != null;

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CacheManualKeys.CLIENT_KEY_PREFIX + id);
            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);

            log.info("Key {} was invalidated. Key {} was incremented.",
                    CacheManualKeys.CLIENT_KEY_PREFIX + id,
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY
            );

            if (profileTouched) {
                redisTemplate.delete(CacheManualKeys.PROFILE_KEY_PREFIX + id);
                versionService.bumpVersion(CacheManualVersionKeys.PROFILES_PAGE_VER_KEY);

                log.info("Key {} was incremented. Key {} was invalidated",
                        CacheManualVersionKeys.PROFILES_PAGE_VER_KEY,
                        CacheManualKeys.PROFILE_KEY_PREFIX + id
                );
            }

            if (couponsTouched) {
                versionService.bumpVersion(CacheManualVersionKeys.COUPON_PAGE_VER_KEY);
                versionService.bumpVersion(CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

                for (Long couponId : couponIds)
                    redisTemplate.delete(CacheManualKeys.COUPON_KEY_PREFIX + couponId);

                log.info("Keys: {}, {} was incremented. Key {} was invalidated.",
                        CacheManualVersionKeys.COUPON_PAGE_VER_KEY,
                        CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,

                        CacheManualKeys.COUPON_KEY_PREFIX
                );
            }

            if (ordersTouched) {
                versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_VER_KEY);
                versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
                versionService.bumpVersion(CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

                for (Long orderId : orderIds)
                    redisTemplate.delete(CacheManualKeys.ORDER_KEY_PREFIX + orderId);

                log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated.",
                        CacheManualVersionKeys.ORDERS_PAGE_VER_KEY,
                        CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                        CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,

                        CacheManualKeys.ORDER_KEY_PREFIX
                );
            }
        });

        return saved;
    }

    @Override
    @Transactional
    public void deleteClient(Long id) {
        log.info("Service called deleteClient with id: {}", id);

        Client clientFromDb = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        List<Long> orderIds = clientFromDb.getOrders().stream().map(Order::getId).toList();
        List<Long> couponIds = clientFromDb.getCoupons().stream().map(Coupon::getId).toList();

        boolean couponsTouched = clientFromDb.getCoupons() != null;
        boolean ordersTouched = clientFromDb.getOrders() != null;

        clientRepository.deleteById(id);

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CacheManualKeys.CLIENT_KEY_PREFIX + id);
            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);

            redisTemplate.delete(CacheManualKeys.PROFILE_KEY_PREFIX + id);
            versionService.bumpVersion(CacheManualVersionKeys.PROFILES_PAGE_VER_KEY);

            log.info("Keys: {}, {} was incremented. Keys: {}, {} was invalidated",
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheManualVersionKeys.PROFILES_PAGE_VER_KEY,

                    CacheManualKeys.CLIENT_KEY_PREFIX + id,
                    CacheManualKeys.PROFILE_KEY_PREFIX + id

            );

            if (couponsTouched)
                versionService.bumpVersion(CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            for (Long couponId : couponIds)
                redisTemplate.delete(CacheManualKeys.COUPON_KEY_PREFIX + couponId);

            log.info("Key {} was incremented. Key {} was invalidated.",
                    CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheManualKeys.COUPON_KEY_PREFIX
            );

            if (ordersTouched) {
                versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_VER_KEY);
                versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
                versionService.bumpVersion(CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

                for (Long orderId : orderIds)
                    redisTemplate.delete(CacheManualKeys.ORDER_KEY_PREFIX + orderId);
            }

            log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated.",
                    CacheManualVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,

                    CacheManualKeys.ORDER_KEY_PREFIX
            );
        });
    }
}
