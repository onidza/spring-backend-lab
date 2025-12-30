package com.onidza.hibernatecore.controller;


import com.onidza.hibernatecore.model.OrderStatus;
import com.onidza.hibernatecore.model.dto.order.OrderDTO;
import com.onidza.hibernatecore.model.dto.order.OrderFilterDTO;
import com.onidza.hibernatecore.service.OrderServiceImpl;
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

    @GetMapping("/order/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
        log.info("Called getOrderById with id: {}", id);
        OrderDTO orderDTO = orderServiceImpl.getOrderById(id);
        return ResponseEntity.ok(orderDTO);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        log.info("Called getAllOrders");
        List<OrderDTO> orders = orderServiceImpl.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<List<OrderDTO>> getAllOrdersByClientId(@PathVariable Long id) {
        log.info("Called getAllOrdersByClientId with id: {}", id);
        List<OrderDTO> orders = orderServiceImpl.getAllOrdersByClientId(id);
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{id}/order")
    public ResponseEntity<OrderDTO> updateOrderByOrderId(@PathVariable Long id,
                                                         @Valid @RequestBody OrderDTO orderDTO) {
        log.info("Called updateOrderByOrderId with id: {}", id);
        OrderDTO order = orderServiceImpl.updateOrderByOrderId(id, orderDTO);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/order")
    public ResponseEntity<OrderDTO> addOrderToClient(@PathVariable Long id,
                                                     @Valid @RequestBody OrderDTO orderDTO) {
        log.info("Called addOrderToClient with id: {}", id);
        OrderDTO order = orderServiceImpl.addOrderToClient(id, orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @DeleteMapping("/{id}/order")
    public ResponseEntity<Void> deleteOrderById(@PathVariable Long id) {
        log.info("Called deleteOrderById with id: {}", id);
        orderServiceImpl.deleteOrderById(id);
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
            @RequestParam(required = false) BigDecimal maxAmount
    ) {
        log.info("Called findOrdersByFilters with status: {}, fromDate {}, toDate {}, minAmount {}, maxAmount {}",
                status, fromDate, toDate, minAmount, maxAmount);

        OrderFilterDTO filter = new OrderFilterDTO(
                status,
                fromDate,
                toDate,
                minAmount,
                maxAmount);

        List<OrderDTO> orders = orderServiceImpl.getOrdersByFilters(filter);
        return ResponseEntity.ok(orders);
    }
}
