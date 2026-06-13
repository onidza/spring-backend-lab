package com.onidza.backend.service.order;

import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;

public interface OrderService {

    OrderDTO getOrder(Long id);

    OrdersPageDTO getOrdersPage(int page, int size);

    OrdersPageDTO getOrdersByClientIdPage(Long id, int page, int size);

    OrderDTO updateOrder(Long id, OrderDTO orderDTO);

    OrderDTO createOrderForClient(Long id, OrderDTO orderDTO);

    void deleteOrder(Long id);

    OrdersPageDTO getOrdersByFilter(OrderFilterDTO filter, int page, int size);
}
