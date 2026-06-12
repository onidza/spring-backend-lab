package com.onidza.backend.controller;


import com.onidza.backend.model.dto.enums.OrderStatus;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;
import com.onidza.backend.service.order.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
@Validated
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/order/{id}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable @Positive Long id
    ) {
        log.info("OrderController called getOrderById with id = {}", id);
        OrderDTO orderDTO = orderService.getOrderById(id);

        return ResponseEntity.ok(orderDTO);
    }

    @GetMapping("/orders")
    public ResponseEntity<OrdersPageDTO> getOrdersPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) @Max(100) int size
    ) {
        log.info("OrderController called getOrdersPage, page = {}, size = {}", page, size);

        return ResponseEntity.ok(orderService.getOrdersPage(page, size));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<OrdersPageDTO> getOrdersPageByClientId(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) @Max(100) int size
    ) {
        log.info("OrderController called getOrdersPageByClientId with id = {}", id);

        return ResponseEntity.ok(orderService.getOrdersPageByClientId(id, page, size));
    }

    @PutMapping("/{id}/order")
    public ResponseEntity<OrderDTO> updateOrderByOrderId(
            @PathVariable @Positive Long id,
            @Valid @RequestBody OrderDTO orderDTO
    ) {
        log.info("OrderController called updateOrderByOrderId with id = {}", id);

        return ResponseEntity.ok(orderService.updateOrderByOrderId(id, orderDTO));
    }

    @PostMapping("/{id}/order")
    public ResponseEntity<OrderDTO> addOrderToClient(
            @PathVariable @Positive Long id,
            @Valid @RequestBody OrderDTO orderDTO
    ) {
        log.info("OrderController called addOrderToClient with id = {}", id);
        OrderDTO order = orderService.addOrderToClientById(id, orderDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @DeleteMapping("/{id}/order")
    public ResponseEntity<Void> deleteOrderById(
            @PathVariable @Positive Long id
    ) {
        log.info("OrderController called deleteOrderById with id = {}", id);
        orderService.deleteOrderByOrderId(id);

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

            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(0) @Max(100) int size
    ) {
        log.info("OrderController called findOrdersByFilters with status = {}, " +
                        "fromDate = {}, toDate = {}, minAmount = {}, maxAmount = {}",
                status, fromDate, toDate, minAmount, maxAmount);

        OrderFilterDTO filter = new OrderFilterDTO(
                status,
                fromDate,
                toDate,
                minAmount,
                maxAmount
        );

        return ResponseEntity.ok(orderService.getOrdersByFilter(filter, page, size));
    }
}
