package com.onidza.backend.service.coupon;

import com.onidza.backend.cache.config.CacheVersionService;
import com.onidza.backend.cache.config.spring.CacheSpringKeys;
import com.onidza.backend.cache.config.spring.CacheSpringVersionKeys;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpringCachingCouponServiceImpl implements CouponService {

    private final MapperService mapperService;
    private final CouponRepository couponRepository;
    private final ClientRepository clientRepository;

    private final TransactionAfterCommitExecutor afterCommitExecutor;
    private final CacheVersionService versionService;
    private final CacheManager cacheManager;

    private static final String COUPON_NOT_FOUND = "Coupon not found";

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheSpringKeys.COUPON_KEY_PREFIX,
            key = "#id",
            condition = "#id > 0"
    )
    public CouponDTO getCouponById(Long id) {
        log.info("Service called getCouponByCouponId with id: {}", id);

        return mapperService.couponToDTO(couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheSpringKeys.COUPON_PAGE_PREFIX,
            keyGenerator = "couponPageKeyGen"
    )
    public CouponPageDTO getCouponsPage(int page, int size) {
        log.info("Service called getCouponsPage");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Slice<CouponDTO> result = couponRepository.findAll(pageable)
                .map(mapperService::couponToDTO);

        return new CouponPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheSpringKeys.COUPONS_PAGE_BY_CLIENT_ID_PREFIX,
            keyGenerator = "couponPageByClientIdKeyGen",
            condition = "#id > 0"
    )
    public CouponPageDTO getCouponsPageByClientId(Long id, int page, int size) {
        log.info("Service called getCouponsPageByClientId with id: {}", id);

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Slice<CouponDTO> result = couponRepository.findByClientsId(id, pageable)
                .map(mapperService::couponToDTO);

        return new CouponPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );
    }

    @Override
    @Transactional
    @CacheEvict(
            cacheNames = CacheSpringKeys.CLIENT_KEY_PREFIX,
            key = "#id",
            condition = "#id > 0"
    )
    public CouponDTO addCouponToClientByClientId(Long id, CouponDTO couponDTO) {
        log.info("Service called addCouponToClientById with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Coupon coupon = mapperService.couponDTOToEntity(couponDTO);
        coupon.getClients().add(client);
        client.getCoupons().add(coupon);

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY);

            log.info("Keys: {}, {}, {} was incremented.",
                    CacheSpringVersionKeys.COUPON_PAGE_VER_KEY,
                    CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY
            );
        });

        return mapperService.couponToDTO(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    @CachePut(
            cacheNames = CacheSpringKeys.COUPON_KEY_PREFIX,
            key = "#result.id()",
            condition = "#id > 0"
    )
    public CouponDTO updateCouponByCouponId(Long id, CouponDTO couponDTO) {
        log.info("Service called updateCouponByCouponId with id: {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND));

        coupon.setCode(couponDTO.code());
        coupon.setDiscount(couponDTO.discount());
        coupon.setExpirationDate(couponDTO.expirationDate());

        CouponDTO result = mapperService.couponToDTO(coupon);
        List<Long> ids = result.clientsId();

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            Cache cache = cacheManager.getCache(CacheSpringKeys.CLIENT_KEY_PREFIX);
            if (cache != null) {
                for (Long clientId : ids) {
                    cache.evict(clientId);
                }
            }
            versionService.bumpVersion(CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY);

            log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated.",
                    CacheSpringVersionKeys.COUPON_PAGE_VER_KEY,
                    CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheSpringKeys.CLIENT_KEY_PREFIX
            );
        });

        return result;
    }

    @Override
    @Transactional
    @CacheEvict(
            cacheNames = CacheSpringKeys.COUPON_KEY_PREFIX,
            key = "#id",
            condition = "#id > 0"
    )
    public void deleteCouponByCouponId(Long id) {
        log.info("Service called deleteCouponById with id: {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND));

        List<Client> clients = coupon.getClients();

        List<Long> ids = clients
                .stream()
                .map(Client::getId)
                .toList();

        clients.forEach(client -> client.getCoupons().remove(coupon));
        clients.clear();

        couponRepository.deleteById(id);

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheSpringVersionKeys.COUPON_PAGE_VER_KEY);
            versionService.bumpVersion(CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY);

            Cache cache = cacheManager.getCache(CacheSpringKeys.CLIENT_KEY_PREFIX);
            if (cache != null) {
                for (Long clientId : ids) {
                    cache.evict(clientId);
                }
            }
            versionService.bumpVersion(CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY);

            log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated.",
                    CacheSpringVersionKeys.COUPON_PAGE_VER_KEY,
                    CacheSpringVersionKeys.COUPONS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheSpringVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheSpringKeys.CLIENT_KEY_PREFIX
            );
        });
    }
}
