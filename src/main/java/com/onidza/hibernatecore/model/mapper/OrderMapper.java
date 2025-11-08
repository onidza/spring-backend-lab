package com.onidza.hibernatecore.model.mapper;

import com.onidza.hibernatecore.model.dto.OrderDTO;
import com.onidza.hibernatecore.model.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final ClientMapper clientMapper;

    public OrderDTO toDTO(Order order) {
        if (order == null) return null;
        return new OrderDTO(
                order.getId(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStatus(),
                clientMapper.toDTO(order.getClient())
        );
    }

    public Order toEntity(OrderDTO orderDTO) {
        if (orderDTO == null) return null;
        return new Order(
                orderDTO.orderDate(),
                orderDTO.totalAmount(),
                orderDTO.status(),
                clientMapper.toEntity(orderDTO.clients())
        );
    }
}
