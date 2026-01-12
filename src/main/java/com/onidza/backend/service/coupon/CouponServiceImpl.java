package com.onidza.backend.service.coupon;

import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Coupon;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final MapperService mapperService;
    private final CouponRepository couponRepository;
    private final ClientRepository clientRepository;

    private static final String COUPON_NOT_FOUND = "Coupon not found";

    @Override
    public CouponDTO getCouponByCouponId(Long id) {
        log.info("Called getCouponById with id: {}", id);

        return mapperService.couponToDTO(couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND)));

    }

    @Override
    public CouponPageDTO getCouponsPage(int page, int size) {
        log.info("Called getCouponsPage");

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
    public CouponPageDTO getCouponsPageByClientId(Long id, int page, int size) {
        log.info("Called getCouponsPageByClientId with id: {}", id);

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
    public CouponDTO addCouponToClientByClientId(Long id, CouponDTO couponDTO) {
        log.info("Called addCouponToClientById with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Coupon coupon = mapperService.couponDTOToEntity(couponDTO);
        coupon.getClients().add(client);
        client.getCoupons().add(coupon);

        return mapperService.couponToDTO(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponDTO updateCouponByCouponId(Long id, CouponDTO couponDTO) {
        log.info("Called updateCouponByCouponId with id: {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND));

        coupon.setCode(couponDTO.code());
        coupon.setDiscount(couponDTO.discount());
        coupon.setExpirationDate(couponDTO.expirationDate());

        return mapperService.couponToDTO(coupon);
    }

    @Override
    @Transactional
    public void deleteCouponByCouponId(Long id) {
        log.info("Called deleteCouponById with id: {}", id);

        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND));

        coupon.getClients()
                .forEach(client -> client.getCoupons().remove(coupon));

        coupon.getClients().clear();
        couponRepository.deleteById(id);
    }
}
