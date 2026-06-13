package com.onidza.backend.service.coupon;

import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;

public interface CouponService {

    CouponDTO getCoupon(Long id);

    CouponPageDTO getCouponsPage(int page, int size);

    CouponPageDTO getCouponsByClientIdPage(Long id, int page, int size);

    CouponDTO createCouponForClient(Long id, CouponDTO couponDTO);

    CouponDTO updateCoupon(Long id, CouponDTO couponDTO);

    void deleteCoupon(Long id);
}
