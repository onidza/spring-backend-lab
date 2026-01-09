package com.onidza.hibernatecore.service.coupon;

import com.onidza.hibernatecore.model.dto.CouponDTO;

import java.util.List;

public interface CouponService {

    CouponDTO getCouponByCouponId(Long id);

    List<CouponDTO> getAllCoupons();

    List<CouponDTO> getAllCouponsByClientId(Long id);

    CouponDTO addCouponToClientByClientId(Long id, CouponDTO couponDTO);

    CouponDTO updateCouponByCouponId(Long id, CouponDTO couponDTO);

    void deleteCouponByCouponId(Long id);
}
