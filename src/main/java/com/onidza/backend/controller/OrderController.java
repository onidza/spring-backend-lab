package com.onidza.backend.controller;


import com.onidza.backend.model.dto.enums.OrderStatus;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;
import com.onidza.backend.service.CacheMode;
import com.onidza.backend.service.order.ManualOrderServiceImpl;
import com.onidza.backend.service.order.OrderService;
import com.onidza.backend.service.order.OrderServiceImpl;
import com.onidza.backend.service.order.SpringCachingOrderServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
public class OrderController {

    private final OrderServiceImpl orderServiceImpl;
    private final ManualOrderServiceImpl manualOrderService;
    private final SpringCachingOrderServiceImpl springCachingOrderService;

    @GetMapping("/order/{id}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getOrderById with id: {}", id);

        OrderService service = resolveOrderService(cacheMode);
        OrderDTO orderDTO = service.getOrderById(id);
        return ResponseEntity.ok(orderDTO);
    }

    @GetMapping("/orders")
    public ResponseEntity<OrdersPageDTO> getOrdersPage(
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Called getOrdersPage");

        OrderService service = resolveOrderService(cacheMode);
        return ResponseEntity.ok(service.getOrdersPage(page, size));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<OrdersPageDTO> getOrdersPageByClientId(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Called getAllOrdersByClientId with id: {}", id);

        OrderService service = resolveOrderService(cacheMode);
        return ResponseEntity.ok(service.getOrdersPageByClientId(id, page, size));
    }

    @PutMapping("/{id}/order")
    public ResponseEntity<OrderDTO> updateOrderByOrderId(
            @PathVariable Long id,
            @Valid @RequestBody OrderDTO orderDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called updateOrderByOrderId with id: {}", id);

        OrderService service = resolveOrderService(cacheMode);
        return ResponseEntity.ok(service.updateOrderByOrderId(id, orderDTO));
    }

    @PostMapping("/{id}/order")
    public ResponseEntity<OrderDTO> addOrderToClient(
            @PathVariable Long id,
            @Valid @RequestBody OrderDTO orderDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called addOrderToClient with id: {}", id);

        OrderService service = resolveOrderService(cacheMode);
        OrderDTO order = service.addOrderToClient(id, orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @DeleteMapping("/{id}/order")
    public ResponseEntity<Void> deleteOrderById(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called deleteOrderById with id: {}", id);

        OrderService service = resolveOrderService(cacheMode);
        service.deleteOrderByOrderId(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders/filtered")
    public ResponseEntity<OrdersPageDTO> findOrdersByFilters(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,

            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,

            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Called findOrdersByFilters with status: {}, fromDate {}, toDate {}, minAmount {}, maxAmount {}",
                status, fromDate, toDate, minAmount, maxAmount);

        OrderFilterDTO filter = new OrderFilterDTO(
                status,
                fromDate,
                toDate,
                minAmount,
                maxAmount);

        OrderService service = resolveOrderService(cacheMode);
        return ResponseEntity.ok(service.getOrdersByFilters(filter, page, size));
    }

    private OrderService resolveOrderService(CacheMode cacheMode) {
        return switch (cacheMode) {
            case NON_CACHE -> orderServiceImpl;
            case MANUAL -> manualOrderService;
            case SPRING -> springCachingOrderService;
        };
    }
}
