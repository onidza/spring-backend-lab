package com.onidza.backend.controller;


import com.onidza.backend.model.OrderStatus;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.service.CacheMode;
import com.onidza.backend.service.order.ManualOrderServiceImpl;
import com.onidza.backend.service.order.OrderService;
import com.onidza.backend.service.order.OrderServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
public class OrderController {

    private final OrderServiceImpl orderServiceImpl;
    private final ManualOrderServiceImpl manualOrderService;

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
    public ResponseEntity<List<OrderDTO>> getAllOrders(
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getAllOrders");

        OrderService service = resolveOrderService(cacheMode);
        List<OrderDTO> orders = service.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<List<OrderDTO>> getAllOrdersByClientId(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getAllOrdersByClientId with id: {}", id);

        OrderService service = resolveOrderService(cacheMode);
        List<OrderDTO> orders = service.getAllOrdersByClientId(id);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}/order")
    public ResponseEntity<OrderDTO> updateOrderByOrderId(
            @PathVariable Long id,
            @Valid @RequestBody OrderDTO orderDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called updateOrderByOrderId with id: {}", id);

        OrderService service = resolveOrderService(cacheMode);
        OrderDTO order = service.updateOrderByOrderId(id, orderDTO);
        return ResponseEntity.ok(order);
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
    public ResponseEntity<List<OrderDTO>> findOrdersByFilters(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,

            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,

            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
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
        List<OrderDTO> orders = service.getOrdersByFilters(filter);
        return ResponseEntity.ok(orders);
    }

    private OrderService resolveOrderService(CacheMode cacheMode) {
        return switch (cacheMode) {
            case NON_CACHE -> orderServiceImpl;
            case MANUAL -> manualOrderService;
            case SPRING -> throw new UnsupportedOperationException("Have no such a service");
        };
    }
}
