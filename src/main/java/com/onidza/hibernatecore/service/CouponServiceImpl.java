package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Coupon;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import com.onidza.hibernatecore.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final MapperService mapperService;
    private final CouponRepository couponRepository;
    private final ClientRepository clientRepository;

    private static final String COUPON_NOT_FOUND = "Coupon not found";

    public CouponDTO getCouponById(Long id) {
        log.info("Called getCouponById with id: {}", id);

        return mapperService.couponToDTO(couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND)));

    }

    public List<CouponDTO> getAllCoupons() {
        log.info("Called getAllCoupons");

        return couponRepository.findAll()
                .stream()
                .map(mapperService::couponToDTO)
                .toList();
    }

    public List<CouponDTO> getAllCouponsByClientId(Long id) {
        log.info("Called getAllCouponsByClientId with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, COUPON_NOT_FOUND));
        return client.getCoupons()
                .stream()
                .map(mapperService::couponToDTO)
                .toList();
    }

    @Transactional
    public CouponDTO addCouponToClientById(Long id, CouponDTO couponDTO) {
        log.info("Called addCouponToClientById with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        Coupon coupon = mapperService.couponDTOToEntity(couponDTO);
        coupon.getClients().add(client);
        client.getCoupons().add(coupon);

        return mapperService.couponToDTO(couponRepository.save(coupon));
    }

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

    @Transactional
    public void deleteCouponById(Long id) {
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
