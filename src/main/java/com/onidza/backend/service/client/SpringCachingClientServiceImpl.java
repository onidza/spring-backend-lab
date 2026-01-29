package com.onidza.backend.service.client;

import com.onidza.backend.cache.config.spring.CacheSpringKeys;
import com.onidza.backend.cache.config.manual.CacheManualVersionKeys;
import com.onidza.backend.cache.config.spring.CacheSpringVersionKeys;
import com.onidza.backend.cache.config.CacheVersionService;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.service.TransactionAfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpringCachingClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final TransactionAfterCommitExecutor afterCommitExecutor;
    private final CacheVersionService versionService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CLIENT_NOT_FOUND = "Client not found";

    @Override
    @Cacheable(
            cacheNames = CacheSpringKeys.CLIENT_KEY_PREFIX,
            key = "#id",
            condition = "#id > 0"
    )
    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long id) {
        log.info("Service called getClientById with id: {}", id);

        return mapperService
                .clientToDTO(clientRepository.findById(id)
                        .orElseThrow(()
                                -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                CLIENT_NOT_FOUND)));
    }

    @Override
    @Cacheable(
            cacheNames = CacheSpringKeys.CLIENTS_PAGE_PREFIX,
            keyGenerator = "clientPageKeyGen"
    )
    @Transactional(readOnly = true)
    public ClientsPageDTO getClientsPage(int page, int size) {
        log.info("Service called getClientsPage");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "id"));

        Page<ClientDTO> result = clientRepository.findAll(pageable)
                .map(mapperService::clientToDTO);

        return new ClientsPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Override
    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        log.info("Service called addClient with name: {}", clientDTO.name());

        Client client = mapperService.clientDTOToEntity(clientDTO);

        client.getProfile().setClient(client);

        Client saved = clientRepository.save(client);

        boolean hasCoupons = clientDTO.coupons() != null && !clientDTO.coupons().isEmpty();
        boolean hasOrders = clientDTO.orders() != null && !clientDTO.orders().isEmpty();

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY);

            log.info("Key {}, {} was incremented.",
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY
            );

            if (hasCoupons) {
                versionService.bumpVersion(CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
                log.info("Keys: {} was incremented.", CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
            }

            if (hasOrders) {
                versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY);
                versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

                log.info("Keys: {}, {} was incremented.",
                        CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY,
                        CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER
                );
            }
        });

        return mapperService.clientToDTO(saved);
    }

    @Override
    @CachePut(
            cacheNames = CacheSpringKeys.CLIENT_KEY_PREFIX,
            key = "#result.id()",
            condition = "#id > 0"
    )
    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        log.info("Service called updateClient with id: {}", id);

        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        CLIENT_NOT_FOUND));

        existing.setName(clientDTO.name());
        existing.setEmail(clientDTO.email());

        boolean couponsTouched = clientDTO.coupons() != null && !clientDTO.coupons().isEmpty();
        boolean orderTouched = clientDTO.orders() != null && !clientDTO.orders().isEmpty();

        Profile existingProfile = existing.getProfile();
        if (existing.getProfile() != null && clientDTO.profile() != null) {
            existingProfile.setAddress(clientDTO.profile().address());
            existingProfile.setPhone(clientDTO.profile().phone());
        }

        if (couponsTouched) {
            existing.getCoupons().clear();
            clientDTO.coupons()
                    .stream()
                    .map(mapperService::couponDTOToEntity)
                    .forEach(coupon -> {
                        existing.getCoupons().add(coupon);
                        coupon.getClients().add(existing);
                    });
        }

        if (orderTouched) {
            existing.getOrders().clear();
            clientDTO.orders()
                    .stream()
                    .map(mapperService::orderDTOToEntity)
                    .forEach(order -> {
                        existing.getOrders().add(order);
                        order.setClient(existing);
                    });
        }

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY);
            redisTemplate.delete(CacheSpringKeys.PROFILE_KEY_PREFIX + id);
            versionService.bumpVersion(CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY);

            log.info("Keys: {}, {} was incremented. Key {} was invalidated.",
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheSpringVersionKeys.PROFILES_PAGE_VER_KEY,
                    CacheSpringKeys.PROFILE_KEY_PREFIX + id
            );

            if (couponsTouched) {
                redisTemplate.delete(CacheSpringKeys.COUPON_KEY_PREFIX + id);
                versionService.bumpVersion(CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
                versionService.bumpVersion(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

                log.info("Keys: {}, {} was incremented. Key {} was invalidated.",
                        CacheSpringVersionKeys.COUPON_PAGE_VER_KEY,
                        CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                        CacheSpringKeys.COUPON_KEY_PREFIX + id
                );
            }

            if (orderTouched) {
                redisTemplate.delete(CacheSpringKeys.ORDER_KEY_PREFIX + id);
                versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY);
                versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
                versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

                log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated.",
                        CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY,
                        CacheSpringVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                        CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,
                        CacheSpringKeys.ORDER_KEY_PREFIX + id
                );
            }
        });

        return mapperService.clientToDTO(clientRepository.save(existing));
    }

    @CacheEvict(
            cacheNames = CacheSpringKeys.CLIENT_KEY_PREFIX,
            key = "#id",
            condition = "#id > 0"
    )
    @Override
    @Transactional
    public void deleteClient(Long id) {
        log.info("Service called deleteClient with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        CLIENT_NOT_FOUND));

        List<Long> ordersId = client.getOrders().stream().map(Order::getId).toList();

        boolean couponsDeleted = client.getCoupons() != null && !client.getCoupons().isEmpty();
        boolean ordersDeleted = client.getOrders() != null && !client.getOrders().isEmpty();

        clientRepository.deleteById(id);

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CacheSpringKeys.CLIENT_KEY_PREFIX + id);
            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);
            redisTemplate.delete(CacheSpringKeys.PROFILE_KEY_PREFIX + id);

            log.info("Key {} was incremented. Key {}, {} was invalidated.",
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheSpringKeys.PROFILE_KEY_PREFIX + id,
                    CacheSpringKeys.CLIENT_KEY_PREFIX + id
            );

            if (couponsDeleted) {
                versionService.bumpVersion(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
                log.info("Key {} was invcremented.", CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
            }

            if (ordersDeleted) {
                for (long orderId : ordersId) {
                    redisTemplate.delete(CacheSpringKeys.ORDER_KEY_PREFIX + orderId);
                }

                versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
                versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY);
                versionService.bumpVersion(CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

                log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated.",
                        CacheSpringKeys.ORDERS_PAGE_BY_CLIENT_ID_PREFIX,
                        CacheSpringVersionKeys.ORDERS_PAGE_VER_KEY,
                        CacheSpringVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,
                        CacheSpringKeys.ORDER_KEY_PREFIX
                );
            }
        });
    }
}
