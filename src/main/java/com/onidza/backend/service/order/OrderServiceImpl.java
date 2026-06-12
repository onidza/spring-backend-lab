package com.onidza.backend.service.order;

import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.model.dto.enums.RetryableTaskType;
import com.onidza.backend.model.dto.order.OrderCreateEvent;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.events.order.OrderAddEvent;
import com.onidza.backend.model.events.order.OrderDeleteEvent;
import com.onidza.backend.model.events.order.OrderUpdateEvent;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.OrderRepository;
import com.onidza.backend.service.retryable.RetryableTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static com.onidza.backend.util.filters.OrderSpecification.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;

    private final ApplicationEventPublisher publisher;
    private final MapperService mapperService;
    private final RetryableTaskService retryableTaskService;

    private static final String ORDER_NOT_FOUND = "Order not found";
    private static final String CLIENT_NOT_FOUND = "Client not found";

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.ORDER_KEY_PREFIX,
            key = "#id"
    )
    public OrderDTO getOrderById(Long id) {
        log.info("OrderServiceImpl called getOrderById with id = {}", id);

        return mapperService.orderToDTO(orderRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.ORDERS_PAGE_PREFIX,
            keyGenerator = "orderPageKeyGen"
    )
    public OrdersPageDTO getOrdersPage(int page, int size) {
        log.info("OrderServiceImpl called getOrdersPage, page = {}, size = {}", page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<OrderDTO> result = orderRepository
                .findAll(pageable)
                .map(mapperService::orderToDTO);

        return new OrdersPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.ORDERS_PAGE_BY_CLIENT_ID_PREFIX,
            keyGenerator = "orderPageByClientIdKeyGen"
    )
    public OrdersPageDTO getOrdersPageByClientId(Long clientId, int page, int size) {
        log.info("OrderServiceImpl called getOrdersPageByClientId with id = {}", clientId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<OrderDTO> result = orderRepository
                .findByClientId(clientId, pageable)
                .map(mapperService::orderToDTO);

        return new OrdersPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Override
    @Transactional
    public OrderDTO addOrderToClientById(Long clientId, OrderDTO orderDTO) {
        log.info("OrderServiceImpl called addOrderToClientById with id = {}", clientId);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        Order order = mapperService.orderDTOToEntity(orderDTO);
        order.setBiClientOrder(client);

        publisher.publishEvent(new OrderAddEvent(clientId));

        OrderCreateEvent kafkaEvent = OrderCreateEvent.builder()
                .clientId(clientId)
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .build();

        retryableTaskService.createRetryableTask(
                kafkaEvent,
                RetryableTaskType.SEND_CREATE_NOTIFICATION_REQUEST
        );

        return mapperService.orderToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    @CachePut(
            cacheNames = CacheKeys.ORDER_KEY_PREFIX,
            key = "#orderId"
    )
    public OrderDTO updateOrderByOrderId(Long orderId, OrderDTO orderDTO) {
        log.info("OrderServiceImpl called updateOrderByOrderId with id = {}", orderId);

        Order existing = orderRepository.findById(orderId)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));

        existing.updateOrder(
                orderDTO.orderDate(),
                orderDTO.totalAmount(),
                orderDTO.status()
        );

        publisher.publishEvent(new OrderUpdateEvent(existing.getClient().getId(), orderId));

        return mapperService.orderToDTO(existing);
    }

    @Override
    @Transactional
    public void deleteOrderByOrderId(Long orderId) {
        log.info("OrderServiceImpl called deleteOrderByOrderId with id = {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));

        order.removeOrderFromClient();

        publisher.publishEvent(new OrderDeleteEvent(order.getClient().getId(), orderId));

        orderRepository.deleteById(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.ORDERS_FILTER_STATUS_KEY_PREFIX,
            keyGenerator = "filterStatusKeyGen",
            condition = "#root.target.isStatusOnlyFilter(filter)"
    )
    public OrdersPageDTO getOrdersByFilter(OrderFilterDTO filter, int page, int size) {
        log.info("OrderServiceImpl called getOrdersByFilter with filter = {}", filter);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id"));

        Specification<Order> specification = hasStatus(filter.status())
                .and(orderDateFrom(filter.fromDate()))
                .and(orderDateTo(filter.toDate()))
                .and(minAmount(filter.minAmount()))
                .and(maxAmount(filter.maxAmount()));

        Page<OrderDTO> result = orderRepository.findAll(specification, pageable)
                .map(mapperService::orderToDTO);

        return new OrdersPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    public boolean isStatusOnlyFilter(OrderFilterDTO filter) {
        return filter.status() != null
                && filter.fromDate() == null
                && filter.toDate() == null
                && filter.minAmount() == null
                && filter.maxAmount() == null;
    }
}
