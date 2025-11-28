package com.onidza.hibernatecore.service.Order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Test
    void getOrderById_returnOrderDTOWithRelations() {
    }

    @Test
    void getOrderById_notFound_throwsException() {
    }

    @Test
    void getAllOrders_returnListOrdersDTOWithRelations() {
    }

    @Test
    void getAllOrders_emptyList() {
    }

    @Test
    void getAllOrdersByClientId_returnListOrdersDTOWithRelations() {
    }

    @Test
    void getAllOrdersByClientId_notFound_throwsException() {
    }

    @Test
    void updateOrderByOrderId_returnOrderDTOWithRelations() {
    }

    @Test
    void updateOrderByOrderId_notFound_throwsExceptions() {
    }

    @Test
    void addOrderToClient_returnOrderDTOWithRelations() {
    }

    @Test
    void addOrderToClient_notFound_throwsExceptions() {
    }

    @Test
    void deleteOrderById_returnNothing() {
    }
}