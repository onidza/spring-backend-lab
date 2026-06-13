package com.onidza.backend.service.coupon;

import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Coupon;
import com.onidza.backend.model.events.coupon.CouponAddEvent;
import com.onidza.backend.model.events.coupon.CouponDeleteEvent;
import com.onidza.backend.model.events.coupon.CouponUpdateEvent;
import com.onidza.backend.model.mappers.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final ClientRepository clientRepository;

    private final ApplicationEventPublisher publisher;
    private final MapperService mapperService;

    private static final String COUPON_NOT_FOUND = "Coupon not found";

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.COUPON_KEY_PREFIX,
            key = "#id"
    )
    public CouponDTO getCoupon(Long id) {
        log.info("CouponServiceImpl called getCoupon with id = {}", id);

        return mapperService.couponToDTO(couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.COUPON_PAGE_PREFIX,
            keyGenerator = "couponPageKeyGen"
    )
    public CouponPageDTO getCouponsPage(int page, int size) {
        log.info("CouponServiceImpl called getCouponsPage, page = {}, size = {}", page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<CouponDTO> result = couponRepository.findAll(pageable)
                .map(mapperService::couponToDTO);

        return new CouponPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.COUPONS_PAGE_BY_CLIENT_ID_PREFIX,
            keyGenerator = "couponPageByClientIdKeyGen"
    )
    public CouponPageDTO getCouponsByClientIdPage(Long clientId, int page, int size) {
        log.info("CouponServiceImpl called getCouponsByClientIdPage with id = {}", clientId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<CouponDTO> result = couponRepository.findDistinctByClientsId(clientId, pageable)
                .map(mapperService::couponToDTO);

        return new CouponPageDTO(
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
    public CouponDTO createCouponForClient(Long clientId, CouponDTO couponDTO) {
        log.info("CouponServiceImpl called createCouponForClient with id = {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Coupon coupon = mapperService.couponDTOToEntity(couponDTO);
        Coupon savedCoupon = couponRepository.save(coupon);

        client.setBiCouponClient(savedCoupon);

        publisher.publishEvent(new CouponAddEvent(clientId));

        return mapperService.couponToDTO(savedCoupon);
    }

    @Override
    @Transactional
    @CachePut(
            cacheNames = CacheKeys.COUPON_KEY_PREFIX,
            key = "#couponId"
    )
    public CouponDTO updateCoupon(Long couponId, CouponDTO couponDTO) {
        log.info("CouponServiceImpl called updateCoupon with id = {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND));

        coupon.updateCoupon(mapperService.couponDTOToEntity(couponDTO));

        CouponUpdateEvent event = buildCouponUpdateEvent(coupon);
        publisher.publishEvent(event);

        return mapperService.couponToDTO(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {
        log.info("CouponServiceImpl called deleteCoupon with id = {}", couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND));

        CouponDeleteEvent event = buildCouponDeleteEvent(coupon);
        publisher.publishEvent(event);

        coupon.deleteCouponFromClients();
        couponRepository.deleteById(couponId);
    }

    private CouponDeleteEvent buildCouponDeleteEvent(Coupon coupon) {
        Set<Long> clientIds = coupon.getClients()
                .stream()
                .map(Client::getId)
                .collect(Collectors.toSet());

        return new CouponDeleteEvent(clientIds, coupon.getId());
    }

    private CouponUpdateEvent buildCouponUpdateEvent(Coupon coupon) {
        Set<Long> clientIds = coupon.getClients()
                .stream()
                .map(Client::getId)
                .collect(Collectors.toSet());

        return new CouponUpdateEvent(clientIds, coupon.getId());
    }
}
