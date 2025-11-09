package com.onidza.hibernatecore.model.mapper;

import com.onidza.hibernatecore.model.dto.OrderDTO;
import com.onidza.hibernatecore.model.entity.Order;
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
