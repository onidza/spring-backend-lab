package com.onidza.backend.service.Order;

import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.OrderRepository;
import com.onidza.backend.service.order.OrderServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OrderServiceUnitTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MapperService mapperService;

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Test
    void getOrderById_returnOrderDTOWithRelations() {
        Order persistentOrder = OrderDataFactory.createPersistentOrderEntity();
        OrderDTO persistentOrderDTO = OrderDataFactory.createPersistentOrderDTO();

        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(persistentOrder));
        Mockito.when(mapperService.orderToDTO(persistentOrder)).thenReturn(persistentOrderDTO);

        OrderDTO result = orderServiceImpl.getOrderById(1L);

        Assertions.assertNotNull(result.id());
        Assertions.assertNotNull(result.totalAmount());

        Assertions.assertEquals(result.status(), persistentOrder.getStatus());
        Assertions.assertEquals(persistentOrder.getOrderDate(), result.orderDate());

        Mockito.verify(orderRepository).findById(1L);
        Mockito.verify(mapperService).orderToDTO(persistentOrder);
    }

    @Test
    void getOrderById_notFound_throwsException() {
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> orderServiceImpl.getOrderById(1L));

        Mockito.verify(orderRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void getAllOrders_returnListOrdersDTOWithRelations() {
        Order persistentOrderEntity = OrderDataFactory.createPersistentOrderEntity();
        Order persistentDistinctOrderEntity = OrderDataFactory.createDistinctPersistentOrderEntity();

        OrderDTO persistentOrderDTO = OrderDataFactory.createPersistentOrderDTO();
        OrderDTO persistentDistinctOrderDTO = OrderDataFactory.createDistinctPersistentOrderDTO();

        Mockito.when(orderRepository.findAll())
                .thenReturn(List.of(persistentOrderEntity, persistentDistinctOrderEntity));
        Mockito.when(mapperService.orderToDTO(persistentOrderEntity))
                .thenReturn(persistentOrderDTO);
        Mockito.when(mapperService.orderToDTO(persistentDistinctOrderEntity))
                .thenReturn(persistentDistinctOrderDTO);

        List<OrderDTO> result = orderServiceImpl.getAllOrders();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(orderDTO
                -> orderDTO.id().equals(1L)));

        Assertions.assertTrue(result.stream().anyMatch(orderDTO
                -> orderDTO.id().equals(2L)));

        Assertions.assertTrue(result.stream().anyMatch(orderDTO
                -> orderDTO.totalAmount().equals(new BigDecimal("1500"))));

        Mockito.verify(orderRepository).findAll();
        Mockito.verify(mapperService, Mockito.times(2))
                .orderToDTO(Mockito.any(Order.class));
    }

    @Test
    void getAllOrders_emptyList() {
        Mockito.when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        List<OrderDTO> result = orderServiceImpl.getAllOrders();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

        Mockito.verify(orderRepository).findAll();
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void getAllOrdersByClientId_returnListOrdersDTOWithRelations() {
        Client persistentClientWithOrders = OrderDataFactory.createPersistClientWithOrders();

        Mockito.when(clientRepository.findById(persistentClientWithOrders.getId()))
                .thenReturn(Optional.of(persistentClientWithOrders));

        Mockito.when(mapperService.orderToDTO(Mockito.any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    return new OrderDTO(
                            order.getId(),
                            order.getOrderDate(),
                            order.getTotalAmount(),
                            order.getStatus(),
                            order.getClient().getId()
                    );
                });

        List<OrderDTO> result = orderServiceImpl.getAllOrdersByClientId(persistentClientWithOrders.getId());

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().allMatch(order ->
            order.clientId().equals(persistentClientWithOrders.getId())
        ));

        Assertions.assertTrue(result.stream().anyMatch(order ->
                order.totalAmount().compareTo(new BigDecimal("1500")) == 0)
        );

        Mockito.verify(clientRepository).findById(persistentClientWithOrders.getId());
        Mockito.verify(mapperService, Mockito.times(2)).orderToDTO(Mockito.any(Order.class));
    }

    @Test
    void getAllOrdersByClientId_notFound_throwsException() {
        Mockito.when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> orderServiceImpl.getAllOrdersByClientId(1L));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void updateOrderByOrderId_returnOrderDTOWithRelations() {
        Order persistOrder = OrderDataFactory.createPersistentOrderEntity();
        OrderDTO forUpdate = OrderDataFactory.createOrderDTOForUpdate();
        OrderDTO orderAfterUpdate = OrderDataFactory.createOrderDTOAfterUpdate();

        Mockito.when(orderRepository.findById(persistOrder.getId()))
                .thenReturn(Optional.of(persistOrder));

        Mockito.when(mapperService.orderToDTO(persistOrder))
                .thenReturn(orderAfterUpdate);

        OrderDTO result = orderServiceImpl.updateOrderByOrderId(persistOrder.getId(), forUpdate);

        Assertions.assertEquals(forUpdate.status(), result.status());
        Assertions.assertEquals(persistOrder.getId(), result.id());
        Assertions.assertEquals(forUpdate.totalAmount(), result.totalAmount());

        Mockito.verify(orderRepository).findById(persistOrder.getId());
        Mockito.verify(mapperService).orderToDTO(persistOrder);
    }

    @Test
    void updateOrderByOrderId_notFound_throwsExceptions() {
        OrderDTO forUpdate = OrderDataFactory.createOrderDTOForUpdate();

        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> orderServiceImpl.updateOrderByOrderId(1L, forUpdate));

        Mockito.verify(orderRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void addOrderToClient_returnOrderDTOWithRelations() {
        Client client = OrderDataFactory.createPersistClientEntityWithEmptyOrders();
        OrderDTO orderDTOForAdd = OrderDataFactory.createOrderDTOForUpdate();
        Order orderEntityForAdd = OrderDataFactory.createPersistentOrderEntityForAdd();
        OrderDTO orderDTOAfterAdd = OrderDataFactory.createOrderDTOAfterUpdate();

        Mockito.when(mapperService.orderDTOToEntity(orderDTOForAdd))
                .thenReturn(orderEntityForAdd);

        Mockito.when(clientRepository.findById(client.getId()))
                .thenReturn(Optional.of(client));

        Mockito.when(orderRepository.save(orderEntityForAdd))
                .thenReturn(orderEntityForAdd);

        Mockito.when(mapperService.orderToDTO(orderEntityForAdd))
                .thenReturn(orderDTOAfterAdd);

        OrderDTO result = orderServiceImpl.addOrderToClient(client.getId(), orderDTOForAdd);

        Assertions.assertEquals(0, result.totalAmount().compareTo(orderDTOForAdd.totalAmount()));
        Assertions.assertEquals(1, client.getOrders().size());

        Mockito.verify(mapperService).orderDTOToEntity(orderDTOForAdd);
        Mockito.verify(clientRepository).findById(client.getId());
        Mockito.verify(orderRepository).save(orderEntityForAdd);
        Mockito.verify(mapperService).orderToDTO(orderEntityForAdd);
    }

    @Test
    void addOrderToClient_ClientNotFound_throwsExceptions() {
        OrderDTO orderDTOForAdd = OrderDataFactory.createOrderDTOForUpdate();

        Mockito.when(clientRepository.findById(1L))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> orderServiceImpl.addOrderToClient(1L, orderDTOForAdd));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
        Mockito.verifyNoInteractions(orderRepository);
    }

    @Test
    void deleteOrderByOrderId_returnNothing() {
        Client clientWithOrders = OrderDataFactory.createPersistClientWithOrders();
        Order orderForDelete = OrderDataFactory.createPersistentOrderEntity();
        orderForDelete.setClient(clientWithOrders);

        Mockito.when(orderRepository.findById(orderForDelete.getId()))
                .thenReturn(Optional.of(orderForDelete));
        Mockito.doNothing()
                .when(orderRepository).deleteById(orderForDelete.getId());

        orderServiceImpl.deleteOrderByOrderId(orderForDelete.getId());

        Mockito.verify(orderRepository).findById(orderForDelete.getId());
        Mockito.verify(orderRepository).deleteById(orderForDelete.getId());
    }
}