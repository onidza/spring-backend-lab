package com.onidza.hibernatecore.model.mapper;

import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Coupon;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

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
                        .collect(Collectors.toList())
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
