package com.onidza.backend.service.order;

import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;

import java.util.List;

public interface OrderService {

    OrderDTO getOrderById(Long id);

    List<OrderDTO> getAllOrders();

    List<OrderDTO> getAllOrdersByClientId(Long id);

    OrderDTO updateOrderByOrderId(Long id, OrderDTO orderDTO);

    OrderDTO addOrderToClient(Long id, OrderDTO orderDTO);

    void deleteOrderById(Long id);

    List<OrderDTO> getOrdersByFilters(OrderFilterDTO filter);
}
