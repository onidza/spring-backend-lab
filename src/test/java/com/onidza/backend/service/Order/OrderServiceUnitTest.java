package com.onidza.backend.service.Order;

import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;
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
import org.springframework.data.domain.*;
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
    void getOrdersPage_returnOrdersPageDTOWithRelations() {
        Order persistentOrderEntity = OrderDataFactory.createPersistentOrderEntity();
        Order persistentDistinctOrderEntity = OrderDataFactory.createDistinctPersistentOrderEntity();

        OrderDTO persistentOrderDTO = OrderDataFactory.createPersistentOrderDTO();
        OrderDTO persistentDistinctOrderDTO = OrderDataFactory.createDistinctPersistentOrderDTO();

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Order> pageFromRepo = new PageImpl<>(
                List.of(persistentOrderEntity, persistentDistinctOrderEntity),
                pageable,
                2
        );

        Mockito.when(orderRepository.findAll(pageable))
                .thenReturn(pageFromRepo);
        Mockito.when(mapperService.orderToDTO(persistentOrderEntity))
                .thenReturn(persistentOrderDTO);
        Mockito.when(mapperService.orderToDTO(persistentDistinctOrderEntity))
                .thenReturn(persistentDistinctOrderDTO);

        OrdersPageDTO result = orderServiceImpl.getOrdersPage(0, 20);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.items().size());
        Assertions.assertEquals(0, result.page());
        Assertions.assertEquals(20, result.size());
        Assertions.assertEquals(2, result.totalElements());
        Assertions.assertEquals(1, result.totalPages());
        Assertions.assertFalse(result.hasNext());

        Assertions.assertTrue(result.items().stream().anyMatch(o -> o.id().equals(1L)));
        Assertions.assertTrue(result.items().stream().anyMatch(o -> o.id().equals(2L)));
        Assertions.assertTrue(result.items().stream()
                .anyMatch(o -> o.totalAmount().compareTo(new BigDecimal("1500")) == 0));

        Mockito.verify(orderRepository).findAll(pageable);
        Mockito.verify(mapperService).orderToDTO(persistentOrderEntity);
        Mockito.verify(mapperService).orderToDTO(persistentDistinctOrderEntity);
        Mockito.verifyNoMoreInteractions(orderRepository, mapperService);
    }

    @Test
    void getOrdersPage_emptyList() {
        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Order> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                pageable,
                0
        );

        Mockito.when(orderRepository.findAll(pageable))
                .thenReturn(emptyPage);

        OrdersPageDTO result = orderServiceImpl.getOrdersPage(0, 20);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.items().isEmpty());
        Assertions.assertEquals(0, result.page());
        Assertions.assertEquals(20, result.size());
        Assertions.assertEquals(0, result.totalElements());
        Assertions.assertEquals(0, result.totalPages());
        Assertions.assertFalse(result.hasNext());

        Mockito.verify(orderRepository).findAll(pageable);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void getOrdersPageByClientId_returnOrdersPageDTOWithRelations() {
        Long clientId = 1L;

        Order persistentOrderEntity = OrderDataFactory.createPersistentOrderEntity();
        Order persistentDistinctOrderEntity = OrderDataFactory.createDistinctPersistentOrderEntity();

        OrderDTO persistentOrderDTO = OrderDataFactory.createPersistentOrderDTOWithId(clientId);
        OrderDTO persistentDistinctOrderDTO = OrderDataFactory.createDistinctPersistentOrderDTOWithId(clientId);

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Order> pageFromRepo = new PageImpl<>(
                List.of(persistentOrderEntity, persistentDistinctOrderEntity),
                pageable,
                2
        );

        Mockito.when(orderRepository.findByClientId(clientId, pageable))
                .thenReturn(pageFromRepo);
        Mockito.when(mapperService.orderToDTO(persistentOrderEntity))
                .thenReturn(persistentOrderDTO);
        Mockito.when(mapperService.orderToDTO(persistentDistinctOrderEntity))
                .thenReturn(persistentDistinctOrderDTO);

        OrdersPageDTO result = orderServiceImpl.getOrdersPageByClientId(clientId, 0, 20);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.items().size());
        Assertions.assertEquals(0, result.page());
        Assertions.assertEquals(20, result.size());
        Assertions.assertEquals(2, result.totalElements());
        Assertions.assertEquals(1, result.totalPages());
        Assertions.assertFalse(result.hasNext());

        Assertions.assertTrue(result.items().stream().allMatch(o -> o.clientId().equals(clientId)));
        Assertions.assertTrue(result.items().stream().anyMatch(o -> o.id().equals(1L)));
        Assertions.assertTrue(result.items().stream().anyMatch(o -> o.id().equals(2L)));
        Assertions.assertTrue(result.items().stream()
                .anyMatch(o -> o.totalAmount().compareTo(new BigDecimal("1500")) == 0));

        Mockito.verify(orderRepository).findByClientId(clientId, pageable);
        Mockito.verify(mapperService).orderToDTO(persistentOrderEntity);
        Mockito.verify(mapperService).orderToDTO(persistentDistinctOrderEntity);
        Mockito.verifyNoMoreInteractions(orderRepository, mapperService);
    }

    @Test
    void getOrdersPageByClientId_notFound_throwsException() {
        Long clientId = 1L;

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Order> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                pageable,
                0
        );

        Mockito.when(orderRepository.findByClientId(clientId, pageable))
                .thenReturn(emptyPage);

        OrdersPageDTO result = orderServiceImpl.getOrdersPageByClientId(clientId, 0, 20);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.items().isEmpty());
        Assertions.assertEquals(0, result.totalElements());
        Assertions.assertEquals(0, result.totalPages());
        Assertions.assertFalse(result.hasNext());

        Mockito.verify(orderRepository).findByClientId(clientId, pageable);
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