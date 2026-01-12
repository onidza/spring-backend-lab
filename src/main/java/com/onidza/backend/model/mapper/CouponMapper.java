package com.onidza.backend.model.mapper;

import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponMapper {

    public CouponDTO toDTO(Coupon coupon) {
        if (coupon == null) return null;
        return new CouponDTO(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscount(),
                coupon.getExpirationDate(),
                coupon.getClients()
                        .stream()
                        .map(Client::getId)
                        .toList()
        );
    }

    public Coupon toEntity(CouponDTO couponDTO) {
        if (couponDTO == null) return null;
        return new Coupon(
                couponDTO.code(),
                couponDTO.discount(),
                couponDTO.expirationDate()
        );
    }
}
