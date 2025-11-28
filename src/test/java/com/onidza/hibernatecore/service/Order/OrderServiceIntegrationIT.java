package com.onidza.hibernatecore.service.Order;

import com.onidza.hibernatecore.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationIT {

    @Autowired
    private OrderService orderService;

    @Test
    void getOrdersByFilters_returnFilteredListOfOrders() {

    }
}
