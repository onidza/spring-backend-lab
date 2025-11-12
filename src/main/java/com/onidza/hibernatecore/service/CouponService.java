package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Coupon;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import com.onidza.hibernatecore.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final MapperService mapperService;
    private final CouponRepository couponRepository;
    private final ClientRepository clientRepository;

    public CouponDTO getCouponById(Long id) {
        return mapperService.couponToDTO(couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found")));

    }

    public List<CouponDTO> getAllCoupons() {
        return couponRepository
                .findAll()
                .stream()
                .map(mapperService::couponToDTO)
                .collect(Collectors.toList());
    }

    public List<CouponDTO> getAllCouponsByClientId(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        return client
                .getCoupons()
                .stream()
                .map(mapperService::couponToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CouponDTO addCouponToClientById(Long id, CouponDTO couponDTO) {
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
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));

        coupon.setCode(couponDTO.code());
        coupon.setDiscount(couponDTO.discount());
        coupon.setExpirationDate(couponDTO.expirationDate());

        return mapperService.couponToDTO(coupon);
    }

    @Transactional
    public void deleteCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));

        coupon.getClients()
                .forEach(client -> client.getCoupons().remove(coupon));

        coupon.getClients().clear();
        couponRepository.deleteById(id);
    }
}
