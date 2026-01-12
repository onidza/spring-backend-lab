package com.onidza.backend.service.Order;

import com.onidza.backend.model.OrderStatus;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;
import com.onidza.backend.service.client.ClientServiceImpl;
import com.onidza.backend.service.order.OrderServiceImpl;
import com.onidza.backend.service.testcontainers.AbstractITConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
class OrderServiceIntegrationIT extends AbstractITConfiguration {

    @Autowired
    private OrderServiceImpl orderServiceImpl;

    @Autowired
    private ClientServiceImpl clientServiceImpl;

    @Test
    void getOrdersByFilters_returnFilteredListOfOrdersWithRelations() {
        ClientDTO inputClientDTO = OrderDataFactory.createInputClientDTO();
        ClientDTO distinctInputClientDTO = OrderDataFactory.createDistinctInputClientDTO();

        clientServiceImpl.addClient(inputClientDTO);
        clientServiceImpl.addClient(distinctInputClientDTO);

        List<OrderDTO> result = orderServiceImpl.getOrdersByFilters(OrderDataFactory.createFilter());

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0, result.get(0).totalAmount().compareTo(new BigDecimal("1500")));
        Assertions.assertEquals(OrderStatus.NEW, result.get(0).status());
        Assertions.assertTrue(result.stream().allMatch(o -> o.status() == OrderStatus.NEW));
        Assertions.assertTrue(result.stream().noneMatch(o -> o.status() == OrderStatus.CANCELLED));

        Assertions.assertTrue(result.stream().allMatch(o ->
                !o.orderDate().isBefore(LocalDateTime.of(2019,1,1,12,0)) &&
                        !o.orderDate().isAfter(LocalDateTime.of(2021,1,1,12,0))
        ));

        Assertions.assertTrue(result.stream().allMatch(o ->
                o.totalAmount().compareTo(BigDecimal.valueOf(100)) >= 0 &&
                        o.totalAmount().compareTo(BigDecimal.valueOf(999999)) <= 0
        ));
    }

    @Test
    void getOrderById_returnOrderDTOWithRelations() {
        ClientDTO inputClientDTO = OrderDataFactory.createInputClientDTO();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        OrderDTO result = orderServiceImpl.getOrderById(saved.orders().get(0).id());

        Assertions.assertEquals(OrderStatus.NEW, result.status());
        Assertions.assertEquals(saved.orders().get(0).id(), result.id());
        Assertions.assertEquals(saved.orders().get(0).orderDate(), result.orderDate());
    }

    @Test
    void getOrdersPage_returnOrdersPageDTOWithRelations() {
        ClientDTO inputClientDTO = OrderDataFactory.createInputClientDTO();
        ClientDTO distinctInputClientDTO = OrderDataFactory.createDistinctInputClientDTO();

        clientServiceImpl.addClient(inputClientDTO);
        clientServiceImpl.addClient(distinctInputClientDTO);

        OrdersPageDTO result = orderServiceImpl.getOrdersPage(0, 20);

        Assertions.assertEquals(2, result.items().size());

        Assertions.assertTrue(result.items().stream()
                .anyMatch(o -> o.totalAmount().compareTo(new BigDecimal("1500")) == 0)
        );

        Set<OrderStatus> statuses = result.items().stream().map(OrderDTO::status).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(OrderStatus.NEW, OrderStatus.CANCELLED), statuses);
    }

    @Test
    void updateOrderById_returnUpdatedOrderDTOWithRelations() {
        ClientDTO inputClientDTO = OrderDataFactory.createInputClientDTO();
        OrderDTO forUpdate = OrderDataFactory.createOrderDTOForUpdate();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        OrderDTO result = orderServiceImpl.updateOrderByOrderId(saved.orders().get(0).id(), forUpdate);

        Assertions.assertEquals(0, result.totalAmount().compareTo(forUpdate.totalAmount()));
        Assertions.assertNotEquals(0, result.totalAmount()
                .compareTo(saved.orders().get(0).totalAmount()));

        Assertions.assertEquals(saved.orders().get(0).id(), result.id());
        Assertions.assertEquals(OrderStatus.CANCELLED, result.status());

        OrderDTO fetched = orderServiceImpl.getOrderById(result.id());
        Assertions.assertEquals(OrderStatus.CANCELLED, fetched.status());
        Assertions.assertEquals(0, fetched.totalAmount().compareTo(forUpdate.totalAmount()));
    }

    @Test
    void addOrderToClient_returnOrderDTOWithRelations() {
        ClientDTO inputClientDTO = OrderDataFactory.createInputClientDTOWithEmptyOrders();
        OrderDTO orderDTOForAdd = OrderDataFactory.createOrderDTOForUpdate();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        OrderDTO result = orderServiceImpl.addOrderToClient(saved.id(), orderDTOForAdd);

        Assertions.assertEquals(saved.id(), result.clientId());
        Assertions.assertEquals(orderDTOForAdd.status(), result.status());
        Assertions.assertEquals(orderDTOForAdd.orderDate(), result.orderDate());
        Assertions.assertEquals(0, result.totalAmount().compareTo(orderDTOForAdd.totalAmount()));

        ClientDTO clientAfter = clientServiceImpl.getClientById(saved.id());
        Assertions.assertEquals(1, clientAfter.orders().size());
        Assertions.assertEquals(result.id(), clientAfter.orders().get(0).id());
    }

    @Test
    void deleteOrderByOrderId_returnNothingWithRelations() {
        ClientDTO inputClientDTO = OrderDataFactory.createInputClientDTO();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        orderServiceImpl.deleteOrderByOrderId(saved.orders().get(0).id());

        Executable exec = () -> orderServiceImpl.getOrderById(saved.orders().get(0).id());
        Assertions.assertThrows(ResponseStatusException.class, exec);

        OrdersPageDTO orders = orderServiceImpl.getOrdersPage(0, 20);
        Assertions.assertTrue(orders.items().stream().noneMatch(o -> o.id().equals(saved.orders().get(0).id())));
    }
}
