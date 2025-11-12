package com.onidza.hibernatecore.controller;


import com.onidza.hibernatecore.model.dto.OrderDTO;
import com.onidza.hibernatecore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
