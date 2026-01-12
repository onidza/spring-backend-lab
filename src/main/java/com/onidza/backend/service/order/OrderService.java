package com.onidza.backend.service.order;

import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;

import java.util.List;

public interface OrderService {

    OrderDTO getOrderById(Long id);

    OrdersPageDTO getOrdersPage(int page, int size);

    OrdersPageDTO getOrdersPageByClientId(Long id, int page, int size);

    OrderDTO updateOrderByOrderId(Long id, OrderDTO orderDTO);

    OrderDTO addOrderToClient(Long id, OrderDTO orderDTO);

    void deleteOrderByOrderId(Long id);

    List<OrderDTO> getOrdersByFilters(OrderFilterDTO filter);
}
