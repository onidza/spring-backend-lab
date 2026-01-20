package com.onidza.backend.service.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Coupon;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.CouponRepository;
import com.onidza.backend.service.TransactionAfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ManualCouponServiceImpl implements CouponService {

    private final MapperService mapperService;
    private final CouponRepository couponRepository;
    private final ClientRepository clientRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private final TransactionAfterCommitExecutor afterCommitExecutor;

    private static final String COUPON_NOT_FOUND = "Coupon not found";
    private static final String CLIENT_NOT_FOUND = "Client not found";

    private static final String COUPON_KEY_PREFIX = "coupon:id:";
    private static final Duration COUPON_TTL = Duration.ofMinutes(1);

    private static final String COUPON_PAGE_VER_KEY = "coupons:all:ver=";
    private static final Duration COUPON_PAGE_VER_TTL = Duration.ofMinutes(1);

    private static final String COUPONS_PAGE_BY_CLIENT_ID_VER_KEY = "coupons:byClientId:";
    private static final Duration COUPONS_PAGE_BY_CLIENT_ID_VER_TTL = Duration.ofMinutes(1);

    private static final String CLIENT_KEY_PREFIX = "client:id:";
    private static final String CLIENTS_PAGE_VER_KEY = "clients:all:ver=";

    @Override
    @Transactional(readOnly = true)
    public CouponDTO getCouponById(Long id) {
        log.info("Service called getCouponById with id: {}", id);

        Object objFromCache = redisTemplate.opsForValue().get(COUPON_KEY_PREFIX + id);

        if (objFromCache != null) {
            log.info("Returned coupon from cache with id: {}", id);
            return objectMapper.convertValue(objFromCache, CouponDTO.class);
        }

        CouponDTO couponDTO = mapperService.couponToDTO(couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND)));

        redisTemplate.opsForValue().set(COUPON_KEY_PREFIX + id, couponDTO, COUPON_TTL);
        log.info("getCouponById was cached...");

        log.info("Returned coupon from db with id: {}", id);
        return couponDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponPageDTO getCouponsPage(int page, int size) {
        log.info("Service called getCouponsPage");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        long ver = couponPageVersion();
        String key = COUPON_PAGE_VER_KEY + ver + "p=" + safePage + ":s=" + safeSize;

        Object objFromCache = redisTemplate.opsForValue().get(key);
        if (objFromCache != null) {
            CouponPageDTO cached = objectMapper.convertValue(objFromCache, CouponPageDTO.class);

            log.info("Returned page from cache with size={}", cached.items().size());
            return cached;
        }

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("id").ascending());

        Slice<CouponDTO> result = couponRepository.findAll(pageable)
                .map(mapperService::couponToDTO);

        CouponPageDTO response = new CouponPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );

        redisTemplate.opsForValue().set(key, response, COUPON_PAGE_VER_TTL);
        log.info("getCouponsPage was cached...");

        log.info("Returned page from db with size={}", response.items().size());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponPageDTO getCouponsPageByClientId(Long id, int page, int size) {
        log.info("Service called getCouponsPageByClientId with id: {}", id);

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        long ver = couponPageByClientIdVersion();
        String key = COUPONS_PAGE_BY_CLIENT_ID_VER_KEY + id + ":ver=" + ver + ":p=" + safePage + ":s=" + safeSize;

        Object objFromCache = redisTemplate.opsForValue().get(key);
        if (objFromCache != null) {
            CouponPageDTO cached = objectMapper.convertValue(objFromCache, CouponPageDTO.class);

            log.info("Returned page from cache with size={}", cached.items().size());
            return cached;
        }

        if (!clientRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("id").ascending());

        Slice<CouponDTO> result = couponRepository.findByClientsId(id, pageable)
                .map(mapperService::couponToDTO);

        CouponPageDTO response = new CouponPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );

        redisTemplate.opsForValue().set(key, response, COUPONS_PAGE_BY_CLIENT_ID_VER_TTL);
        log.info("getCouponsPageByClientId was cached...");

        log.info("Returned page: {} from db with size: {}", id, result.getContent().size());

        return response;
    }

    @Override
    @Transactional
    public CouponDTO addCouponToClientByClientId(Long id, CouponDTO couponDTO) {
        log.info("Service called addCouponToClientById with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        Coupon coupon = mapperService.couponDTOToEntity(couponDTO);
        coupon.getClients().add(client);
        client.getCoupons().add(coupon);

        Coupon saved = couponRepository.save(coupon);

        afterCommitExecutor.run(() -> {
            bumpCouponPageVer();
            bumpCouponPageByClientIdVer();

            redisTemplate.delete(CLIENT_KEY_PREFIX + id);
            bumpClientPageVer();

            log.info("Version of key={} was incremented", COUPON_PAGE_VER_KEY);
            log.info("Version of key={} was incremented", COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            log.info("Added a new coupon, getClientById was invalidated with key={}", CLIENT_KEY_PREFIX + id);
            log.info("Version of key={} was incremented", CLIENTS_PAGE_VER_KEY);
        });

        return mapperService.couponToDTO(saved);
    }

    @Override //TODO
    @Transactional
    public CouponDTO updateCouponByCouponId(Long id, CouponDTO couponDTO) {
        log.info("Service called updateCouponByCouponId with id: {}", id);

        List<Long> cacheKeyClientKeys = couponRepository.findClientIdsByCouponId(id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND));

        coupon.setCode(couponDTO.code());
        coupon.setDiscount(couponDTO.discount());
        coupon.setExpirationDate(couponDTO.expirationDate());

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(COUPON_KEY_PREFIX + id);
            redisTemplate.delete(COUPON_PAGE_VER_KEY);

            for (Long clientId : cacheKeyClientKeys) {
                redisTemplate.delete(CLIENT_KEY_PREFIX + clientId);
                redisTemplate.delete(COUPONS_PAGE_BY_CLIENT_ID_VER_KEY + clientId);
            }
            redisTemplate.delete(CLIENTS_PAGE_VER_KEY);

            log.info("Updated coupon was invalidated in cache with key={}", COUPON_KEY_PREFIX + id);
            log.info("Updated coupon in getAllList was invalidated too with key={}", COUPON_PAGE_VER_KEY);

            log.info("Invalidated {} client caches due to coupon update: clientIds={}",
                    cacheKeyClientKeys.size(),
                    cacheKeyClientKeys);
            log.info("Updated coupon in getAllCouponsByClientId was invalidated too with key={}", COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
            log.info("Updated coupon in getAllClients was invalidated with key={}", CLIENTS_PAGE_VER_KEY);
        });

        return mapperService.couponToDTO(coupon);
    }

    @Override
    @Transactional
    public void deleteCouponByCouponId(Long id) {
        log.info("Service called deleteCouponById with id: {}", id);

        List<Long> cacheKeyClientKeys = couponRepository.findClientIdsByCouponId(id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND));

        coupon.getClients()
                .forEach(client -> client.getCoupons().remove(coupon));

        coupon.getClients().clear();
        couponRepository.deleteById(id);

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(COUPON_KEY_PREFIX + id);
            redisTemplate.delete(COUPON_PAGE_VER_KEY);

            for (Long clientId : cacheKeyClientKeys) {
                redisTemplate.delete(CLIENT_KEY_PREFIX + clientId);
                redisTemplate.delete(COUPONS_PAGE_BY_CLIENT_ID_VER_KEY + clientId);
            }
            redisTemplate.delete(CLIENTS_PAGE_VER_KEY);

            log.info("Deleted coupon was invalidated in cache with key={}", COUPON_KEY_PREFIX + id);
            log.info("Deleted coupon in getAllList was invalidated too with key={}", COUPON_PAGE_VER_KEY);

            log.info("Invalidated {} client caches due to coupon delete: clientIds={}",
                    cacheKeyClientKeys.size(),
                    cacheKeyClientKeys);
            log.info("Deleted coupon in getAllCouponsByClientId was invalidated too with key={}", COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
            log.info("Deleted coupon in getAllClients was invalidated with key={}", CLIENTS_PAGE_VER_KEY);
        });
    }

    private long couponPageVersion() {
        Long ver = stringRedisTemplate.opsForValue()
                .increment(COUPON_PAGE_VER_KEY, 0);

        return ver == null ? 0 : ver;
    }

    private void bumpCouponPageVer() {
        stringRedisTemplate.opsForValue()
                .increment(COUPON_PAGE_VER_KEY);
    }

    private long couponPageByClientIdVersion() {
        Long ver = stringRedisTemplate.opsForValue()
                .increment(COUPONS_PAGE_BY_CLIENT_ID_VER_KEY, 0);

        return ver == null ? 0 : ver;
    }

    private void bumpCouponPageByClientIdVer() {
        stringRedisTemplate.opsForValue()
                .increment(COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
    }

    private long clientPageVersion() {
        Long ver = stringRedisTemplate.opsForValue()
                .increment(CLIENTS_PAGE_VER_KEY, 0);

        return ver == null ? 0 : ver;
    }

    private void bumpClientPageVer() {
        stringRedisTemplate.opsForValue()
                .increment(CLIENTS_PAGE_VER_KEY);
    }
}
