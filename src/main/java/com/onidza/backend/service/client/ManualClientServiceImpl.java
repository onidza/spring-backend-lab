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

    private static final String CLIENT_KEY_PREFIX = "client:id:";
    private static final long CLIENT_TTL_MINUTES = 1;

    private static final String CLIENTS_PAGE_VER_KEY = "clients:all:ver=";
    private static final Duration PAGE_CLIENTS_TTL = Duration.ofMinutes(1);

    private static final String COUPON_PAGE_VER_KEY = "coupons:all:ver=";
    private static final String COUPONS_PAGE_BY_CLIENT_ID_VER_KEY = "coupons:byClientId:";

    private static final String CLIENT_NOT_FOUND = "Client not found";

    @Override
    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long id) {
        log.info("Service called getClientById with id: {}", id);

        String objFromCache = stringRedisTemplate.opsForValue().get(CLIENT_KEY_PREFIX + id);

        try {
            if (objFromCache != null) {
                log.info("Returned client from cache with id: {}", id);
                return objectMapper.readValue(objFromCache, ClientDTO.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to read client from cache with key {}", CLIENT_KEY_PREFIX + id, e);
        }

        Client clientFromDb = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        try {
            stringRedisTemplate.opsForValue().set(
                    CLIENT_KEY_PREFIX + id,
                    objectMapper.writeValueAsString(mapperService.clientToDTO(clientFromDb)),
                    CLIENT_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
            log.info("getClientById was cached...");

        } catch (JsonProcessingException e) {
            log.warn("Failed to write client to cache with key {}", CLIENT_KEY_PREFIX + id, e);
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

        long ver = clientPageVersion();
        String key = CLIENTS_PAGE_VER_KEY + ver + ":p=" + safePage + ":s=" + safeSize;

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
            bumpClientPageVer();
            log.info("Key {} was incremented", CLIENTS_PAGE_VER_KEY);

            if (hasCoupons) {
                bumpCouponPageVer();
                bumpCouponPageByClientIdVer();
                log.info("Keys: {}, {} was incremented.",
                        COUPON_PAGE_VER_KEY,
                        COUPONS_PAGE_BY_CLIENT_ID_VER_KEY
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
            redisTemplate.delete(CLIENT_KEY_PREFIX + id);
            bumpClientPageVer();

            log.info("Key {} was invalidated. Key {} was incremented.",
                    CLIENT_KEY_PREFIX + id,
                    COUPONS_PAGE_BY_CLIENT_ID_VER_KEY
            );

            if (couponsTouched) {
                bumpCouponPageVer();
                bumpCouponPageByClientIdVer();

                log.info("Keys: {}, {} was incremented.",
                        COUPON_PAGE_VER_KEY,
                        COUPONS_PAGE_BY_CLIENT_ID_VER_KEY
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
            redisTemplate.delete(CLIENT_KEY_PREFIX + id);
            bumpClientPageVer();

            bumpCouponPageVer();
            bumpCouponPageByClientIdVer();

            log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated",
                    CLIENT_KEY_PREFIX + id,
                    CLIENTS_PAGE_VER_KEY,

                    COUPON_PAGE_VER_KEY,
                    COUPONS_PAGE_BY_CLIENT_ID_VER_KEY
            );
        });
    }


    //TODO
    private long clientPageVersion() {
        Long ver = stringRedisTemplate.opsForValue()
                .increment(CLIENTS_PAGE_VER_KEY, 0);

        return ver == null ? 0 : ver;
    }

    private void bumpClientPageVer() {
        stringRedisTemplate.opsForValue()
                .increment(CLIENTS_PAGE_VER_KEY);
    }

    private void bumpCouponPageVer() {
        stringRedisTemplate.opsForValue()
                .increment(COUPON_PAGE_VER_KEY);
    }

    private void bumpCouponPageByClientIdVer() {
        stringRedisTemplate.opsForValue()
                .increment(COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
    }
}
