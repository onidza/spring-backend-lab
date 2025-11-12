package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.OrderDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Order;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import com.onidza.hibernatecore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    public OrderDTO getOrderById(Long id) {
        return mapperService.orderToDTO(orderRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")));
    }

    public List<OrderDTO> getAllOrders() {
        return orderRepository
                .findAll()
                .stream()
                .map(mapperService::orderToDTO)
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getAllOrdersByClientId(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        return client
                .getOrders()
                .stream()
                .map(mapperService::orderToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO updateOrderByOrderId(Long id, OrderDTO orderDTO) {
        Order order = orderRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        order.setTotalAmount(orderDTO.totalAmount());
        order.setStatus(orderDTO.status());
        order.setOrderDate(orderDTO.orderDate());

        return mapperService.orderToDTO(order);
    }

    @Transactional
    public OrderDTO addOrderToClient(Long id, OrderDTO orderDTO) {
        Order order = mapperService.orderDTOToEntity(orderDTO);
        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        order.setClient(client);

        return mapperService.orderToDTO(orderRepository.save(order));
    }

    public void deleteOrderById(Long id) {
        orderRepository.deleteById(id);
    }
}
