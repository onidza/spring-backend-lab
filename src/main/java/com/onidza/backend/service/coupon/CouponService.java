package com.onidza.backend.service.coupon;

import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;

public interface CouponService {

    CouponDTO getCouponByCouponId(Long id);

    CouponPageDTO getCouponsPage(int page, int size);

    CouponPageDTO getCouponsPageByClientId(Long id, int page, int size);

    CouponDTO addCouponToClientByClientId(Long id, CouponDTO couponDTO);

    CouponDTO updateCouponByCouponId(Long id, CouponDTO couponDTO);

    void deleteCouponByCouponId(Long id);
}
