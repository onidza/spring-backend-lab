package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.CouponDTO;

import java.util.List;

public interface CouponService {

    CouponDTO getCouponById(Long id);

    List<CouponDTO> getAllCoupons();

    List<CouponDTO> getAllCouponsByClientId(Long id);

    CouponDTO addCouponToClientById(Long id, CouponDTO couponDTO);

    CouponDTO updateCouponByCouponId(Long id, CouponDTO couponDTO);

    void deleteCouponById(Long id);
}
