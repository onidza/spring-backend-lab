package com.onidza.hibernatecore.controller;

import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/coupon/{id}")
    public CouponDTO getCouponById(@PathVariable Long id) {
        return couponService.getCouponById(id);
    }

    @GetMapping("/coupons")
    public List<CouponDTO> getAllCoupons() {
        return couponService.getAllCoupons();
    }

    @GetMapping("/{id}/coupons")
    public List<CouponDTO> getAllCouponsByClientId(@PathVariable Long id) {
        return couponService.getAllCouponsByClientId(id);
    }

    @PostMapping("/{id}/coupons")
    public CouponDTO addCouponToClientById(@PathVariable Long id,
                                           @Valid @RequestBody CouponDTO couponDTO) {
        return couponService.addCouponToClientById(id, couponDTO);
    }

    @PutMapping("/{id}/coupons")
    public CouponDTO updateCouponByCouponId(@PathVariable Long id,
                                            @Valid @RequestBody CouponDTO couponDTO) {
        return couponService.updateCouponByCouponId(id, couponDTO);
    }

    @DeleteMapping("/{id}/coupon")
    public void deleteCouponById(@PathVariable Long id) {
        couponService.deleteCouponById(id);
    }
}
