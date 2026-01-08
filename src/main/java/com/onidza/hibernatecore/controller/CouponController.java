package com.onidza.hibernatecore.controller;

import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.service.CacheMode;
import com.onidza.hibernatecore.service.coupon.CouponService;
import com.onidza.hibernatecore.service.coupon.CouponServiceImpl;
import com.onidza.hibernatecore.service.coupon.ManualCouponServiceImpl;
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

    private final CouponServiceImpl couponServiceImpl;
    private final ManualCouponServiceImpl manualCouponService;

    @GetMapping("/coupon/{id}")
    public ResponseEntity<CouponDTO> getCouponById(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getCouponById with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        CouponDTO couponDTO = service.getCouponById(id);

        return ResponseEntity.ok(couponDTO);
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<CouponDTO>> getAllCoupons(
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getAllCoupons");

        CouponService service = resolveCouponService(cacheMode);
        List<CouponDTO> coupons = service.getAllCoupons();

        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/{id}/coupons")
    public ResponseEntity<List<CouponDTO>> getAllCouponsByClientId(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getAllCouponsByClientId with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        List<CouponDTO> coupons = service.getAllCouponsByClientId(id);

        return ResponseEntity.ok(coupons);
    }

    @PostMapping("/{id}/coupons")
    public ResponseEntity<CouponDTO> addCouponToClientById(
            @PathVariable Long id,
            @Valid @RequestBody CouponDTO couponDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called addCouponToClientById with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        CouponDTO coupon = service.addCouponToClientById(id, couponDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(coupon);
    }

    @PutMapping("/{id}/coupons")
    public ResponseEntity<CouponDTO> updateCouponByCouponId(
            @PathVariable Long id,
            @Valid @RequestBody CouponDTO couponDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called updateCouponByCouponId with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        CouponDTO coupon = service.updateCouponByCouponId(id, couponDTO);

        return ResponseEntity.ok(coupon);
    }

    @DeleteMapping("/{id}/coupon")
    public ResponseEntity<Void> deleteCouponById(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called deleteCouponById with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        service.deleteCouponByCouponId(id);

        return ResponseEntity.noContent().build();
    }

    private CouponService resolveCouponService(CacheMode cacheMode) {
        return switch (cacheMode) {
            case NON_CACHE -> couponServiceImpl;
            case MANUAL -> manualCouponService;
            case SPRING -> throw new UnsupportedOperationException("Have no such a service");
        };
    }
}
