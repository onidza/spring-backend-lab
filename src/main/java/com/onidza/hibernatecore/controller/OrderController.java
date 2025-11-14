package com.onidza.hibernatecore.controller;


import com.onidza.hibernatecore.model.OrderStatus;
import com.onidza.hibernatecore.model.dto.order.OrderDTO;
import com.onidza.hibernatecore.model.dto.order.OrderFilterDTO;
import com.onidza.hibernatecore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/order/{id}")
    public OrderDTO getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/orders")
    public List<OrderDTO> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}/orders")
    public List<OrderDTO> getAllOrdersByClientId(@PathVariable Long id) {
        return orderService.getAllOrdersByClientId(id);
    }

    @PutMapping("/{id}/order")
    public OrderDTO updateOrderByOrderId(@PathVariable Long id,
                                         @Valid @RequestBody OrderDTO orderDTO) {
        return orderService.updateOrderByOrderId(id, orderDTO);
    }

    @PostMapping("/{id}/order")
    public OrderDTO addOrderToClient(@PathVariable Long id,
                                     @Valid @RequestBody OrderDTO orderDTO) {
        return orderService.addOrderToClient(id, orderDTO);
    }

    @DeleteMapping("/{id}/order")
    public void deleteOrderById(@PathVariable Long id) {
        orderService.deleteOrderById(id);
    }

    @GetMapping("/orders/filtered")
    public List<OrderDTO> findOrdersByFilters(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,

            @RequestParam(required = false)BigDecimal minAmount,
            @RequestParam(required = false)BigDecimal maxAmount
            ) {

        OrderStatus orderStatus = (status == null) ? null : OrderStatus.valueOf(status.toUpperCase());

        OrderFilterDTO filter = new OrderFilterDTO(
                orderStatus,
                fromDate,
                toDate,
                minAmount,
                maxAmount);

        return orderService.getOrdersByFilters(filter);
    }
}
