package com.onidza.backend.model.mapper;

import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderDTO toDTO(Order order) {
        if (order == null) return null;

        return new OrderDTO(
                order.getId(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getClient().getId()
        );
    }

    public Order toEntity(OrderDTO orderDTO) {
        if (orderDTO == null) return null;

        return new Order(
                orderDTO.orderDate(),
                orderDTO.totalAmount(),
                orderDTO.status()
        );
    }
}
