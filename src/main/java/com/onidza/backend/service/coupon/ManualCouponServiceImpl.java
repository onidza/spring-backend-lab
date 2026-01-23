package com.onidza.backend.service.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.cache.config.manual.CacheManualKeys;
import com.onidza.backend.cache.config.manual.CacheManualVersionKeys;
import com.onidza.backend.cache.config.manual.CacheTtlProps;
import com.onidza.backend.cache.config.CacheVersionService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
public class ManualCouponServiceImpl implements CouponService {

    private final MapperService mapperService;
    private final CouponRepository couponRepository;
    private final ClientRepository clientRepository;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final CacheTtlProps ttlProps;
    private final CacheVersionService versionService;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    private static final String COUPON_NOT_FOUND = "Coupon not found";
    private static final String CLIENT_NOT_FOUND = "Client not found";

    @Override
    @Transactional(readOnly = true)
    public CouponDTO getCouponById(Long id) {
        log.info("Service called getCouponById with id: {}", id);

        Object objFromCache = redisTemplate.opsForValue().get(CacheManualKeys.COUPON_KEY_PREFIX + id);

        if (objFromCache != null) {
            log.info("Returned coupon from cache with id: {}", id);
            return objectMapper.convertValue(objFromCache, CouponDTO.class);
        }

        CouponDTO couponDTO = mapperService.couponToDTO(couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND)));

        redisTemplate.opsForValue().set(CacheManualKeys.COUPON_KEY_PREFIX + id,
                couponDTO, ttlProps.getCouponById());
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

        long ver = versionService.getKeyVersion(CacheManualVersionKeys.COUPON_PAGE_VER_KEY );
        String key = CacheManualKeys.COUPON_PAGE_PREFIX + ver + ":p=" + safePage + ":s=" + safeSize;

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

        redisTemplate.opsForValue().set(key, response, ttlProps.getCouponsPage());
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

        long ver = versionService.getKeyVersion(CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
        String key = CacheManualKeys.COUPONS_PAGE_BY_CLIENT_ID_PREFIX + id + ":ver=" + ver + ":p=" + safePage + ":s=" + safeSize;

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

        redisTemplate.opsForValue().set(key, response, ttlProps.getCouponsPageByClientId());
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
            versionService.bumpVersion(CacheManualVersionKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);
            redisTemplate.delete(CacheManualKeys.CLIENT_KEY_PREFIX + id);

            log.info("Keys: {}, {}, {}, {} was incremented.",
                    CacheManualVersionKeys.COUPON_PAGE_VER_KEY,
                    CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheManualKeys.CLIENT_KEY_PREFIX + id
            );
        });

        return mapperService.couponToDTO(saved);
    }

    @Override
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

        Coupon saved = couponRepository.save(coupon);

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CacheManualKeys.COUPON_KEY_PREFIX + id);
            versionService.bumpVersion(CacheManualVersionKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);

            for (Long clientId : cacheKeyClientKeys)
                redisTemplate.delete(CacheManualKeys.CLIENT_KEY_PREFIX + clientId);

            log.info("Keys: {}, {}, {}, {} was incremented. Client with keys (size={}) was invalidated.",
                    CacheManualKeys.COUPON_KEY_PREFIX + id,
                    CacheManualVersionKeys.COUPON_PAGE_VER_KEY,
                    CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                    cacheKeyClientKeys.size()
            );
        });

        return mapperService.couponToDTO(saved);
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
            redisTemplate.delete(CacheManualKeys.COUPON_KEY_PREFIX + id);
            versionService.bumpVersion(CacheManualVersionKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);

            for (Long clientId : cacheKeyClientKeys)
                redisTemplate.delete(CacheManualKeys.CLIENT_KEY_PREFIX + clientId);

            log.info("Keys: {}, {}, {}, {} was incremented. Client with keys (size={}) was invalidated.",
                    CacheManualKeys.COUPON_KEY_PREFIX + id,
                    CacheManualVersionKeys.COUPON_PAGE_VER_KEY,
                    CacheManualVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,
                    cacheKeyClientKeys.size()
            );
        });
    }
}
