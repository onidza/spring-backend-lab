package com.onidza.backend.controller;

import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;
import com.onidza.backend.service.CacheMode;
import com.onidza.backend.service.coupon.CouponService;
import com.onidza.backend.service.coupon.CouponServiceImpl;
import com.onidza.backend.service.coupon.ManualCouponServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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
        log.info("Controller called getCouponById with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        CouponDTO couponDTO = service.getCouponById(id);

        return ResponseEntity.ok(couponDTO);
    }

    @GetMapping("/coupons")
    public ResponseEntity<CouponPageDTO> getCouponsPage(
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Controller called getCouponsPage, page={}, size={}", page, size);

        CouponService service = resolveCouponService(cacheMode);
        CouponPageDTO coupons = service.getCouponsPage(page, size);

        return ResponseEntity.ok(coupons);
    }

    @GetMapping("/{id}/coupons")
    public ResponseEntity<CouponPageDTO> getCouponsPageByClientId(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Controller called getCouponsPageByClientId with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        CouponPageDTO coupons = service.getCouponsPageByClientId(id, page, size);

        return ResponseEntity.ok(coupons);
    }

    @PostMapping("/{id}/coupons")
    public ResponseEntity<CouponDTO> addCouponToClientById(
            @PathVariable Long id,
            @Valid @RequestBody CouponDTO couponDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Controller called addCouponToClientById with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        CouponDTO coupon = service.addCouponToClientByClientId(id, couponDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(coupon);
    }

    @PutMapping("/{id}/coupons")
    public ResponseEntity<CouponDTO> updateCouponByCouponId(
            @PathVariable Long id,
            @Valid @RequestBody CouponDTO couponDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Controller called updateCouponByCouponId with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        CouponDTO coupon = service.updateCouponByCouponId(id, couponDTO);

        return ResponseEntity.ok(coupon);
    }

    @DeleteMapping("/{id}/coupon")
    public ResponseEntity<Void> deleteCouponById(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Controller called deleteCouponById with id: {}", id);

        CouponService service = resolveCouponService(cacheMode);
        service.deleteCouponByCouponId(id);

        return ResponseEntity.noContent().build();
    }

    private CouponService resolveCouponService(CacheMode cacheMode) {
        return switch (cacheMode) {
            case NON_CACHE -> couponServiceImpl;
            case MANUAL -> manualCouponService;
            case SPRING -> throw new UnsupportedOperationException("Have no such a service"); //TODO
        };
    }
}
