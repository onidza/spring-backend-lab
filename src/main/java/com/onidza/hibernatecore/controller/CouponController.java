package com.onidza.hibernatecore.controller;

import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/coupon/{id}")
    public ResponseEntity<CouponDTO> getCouponById(@PathVariable Long id) {
        log.info("Called getCouponById with id: {}", id);
        CouponDTO couponDTO = couponService.getCouponById(id);
        return ResponseEntity.ok(couponDTO);
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<CouponDTO>> getAllCoupons() {
        log.info("Called getAllCoupons");
        List<CouponDTO> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/{id}/coupons")
    public ResponseEntity<List<CouponDTO>> getAllCouponsByClientId(@PathVariable Long id) {
        log.info("Called getAllCouponsByClientId with id: {}", id);
        List<CouponDTO> coupons = couponService.getAllCouponsByClientId(id);
        return ResponseEntity.ok(coupons);
    }

    @PostMapping("/{id}/coupons")
    public ResponseEntity<CouponDTO> addCouponToClientById(@PathVariable Long id,
                                           @Valid @RequestBody CouponDTO couponDTO) {
        log.info("Called addCouponToClientById with id: {}", id);
        CouponDTO coupon = couponService.addCouponToClientById(id, couponDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(coupon);
    }

    @PutMapping("/{id}/coupons")
    public ResponseEntity<CouponDTO> updateCouponByCouponId(@PathVariable Long id,
                                                            @Valid @RequestBody CouponDTO couponDTO) {
        log.info("Called updateCouponByCouponId with id: {}", id);
        CouponDTO coupon = couponService.updateCouponByCouponId(id, couponDTO);
        return ResponseEntity.ok(coupon);
    }

    @DeleteMapping("/{id}/coupon")
    public ResponseEntity<Void> deleteCouponById(@PathVariable Long id) {
        log.info("Called deleteCouponById with id: {}", id);
        couponService.deleteCouponById(id);
        return ResponseEntity.noContent().build();
    }
}
