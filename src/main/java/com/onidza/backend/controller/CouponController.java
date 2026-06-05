package com.onidza.backend.controller;

import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;
import com.onidza.backend.service.coupon.CouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
@Validated
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/coupon/{id}")
    public ResponseEntity<CouponDTO> getCouponById(
            @PathVariable @Positive Long id
    ) {
        log.info("CouponController called getCouponById with id = {}", id);
        CouponDTO couponDTO = couponService.getCouponById(id);
        return ResponseEntity.ok(couponDTO);
    }

    @GetMapping("/coupons")
    public ResponseEntity<CouponPageDTO> getCouponsPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("CouponController called getCouponsPage, page = {}, size = {}", page, size);
        CouponPageDTO coupons = couponService.getCouponsPage(page, size);

        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/{id}/coupons")
    public ResponseEntity<CouponPageDTO> getCouponsPageByClientId(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("CouponController called getCouponsPageByClientId with id = {}", id);
        CouponPageDTO coupons = couponService.getCouponsPageByClientId(id, page, size);

        return ResponseEntity.ok(coupons);
    }

    @PostMapping("/{id}/coupons")
    public ResponseEntity<CouponDTO> addCouponByClientId(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CouponDTO couponDTO
    ) {
        log.info("CouponController called addCouponByClientId for client with id = {}", id);
        CouponDTO coupon = couponService.addCouponToClientByClientId(id, couponDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(coupon);
    }

    @PutMapping("/{id}/coupons")
    public ResponseEntity<CouponDTO> updateCouponByCouponId(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CouponDTO couponDTO
    ) {
        log.info("CouponController called updateCouponByCouponId with id = {}", id);
        CouponDTO coupon = couponService.updateCouponByCouponId(id, couponDTO);

        return ResponseEntity.ok(coupon);
    }

    @DeleteMapping("/{id}/coupon")
    public ResponseEntity<Void> deleteCouponById(
            @PathVariable @Positive Long id
    ) {
        log.info("CouponController called deleteCouponById with id = {}", id);
        couponService.deleteCouponByCouponId(id);

        return ResponseEntity.noContent().build();
    }
}
