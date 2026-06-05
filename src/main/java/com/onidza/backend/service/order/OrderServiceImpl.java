package com.onidza.backend.service.order;

import com.onidza.backend.model.dto.client.events.ActionPart;
import com.onidza.backend.model.dto.client.events.profile.ProfileAddEvent;
import com.onidza.backend.service.cache.CacheVersionService;
import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.config.cache.keys.CacheVersionKeys;
import com.onidza.backend.model.dto.enums.RetryableTaskType;
import com.onidza.backend.model.dto.order.OrderCreateEvent;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.OrderRepository;
import com.onidza.backend.service.retryable.RetryableTaskService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
        log.info("OrderServiceImpl getOrderById with id = {}", id);

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

        Page<OrderDTO> result = orderRepository.findAll(pageable).map(mapperService::orderToDTO);

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
    public OrdersPageDTO getOrdersPageByClientId(Long id, int page, int size) {
        log.info("OrderServiceImpl called getOrdersPageByClientId with id = {}", id);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<OrderDTO> result = orderRepository
                .findByClientId(id, pageable)
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
    public OrderDTO addOrderToClient(Long id, OrderDTO orderDTO) {
        log.info("OrderServiceImpl called addOrderToClient with id = {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        Order order = mapperService.orderDTOToEntity(orderDTO);
        order.setBiClientOrder(client);

        EnumSet<ActionPart> parts = EnumSet.noneOf(ActionPart.class);

        if (orderDTO.clientId() != null)
            parts.add(ActionPart.CLIENT);

        parts.add(ActionPart.ORDERS);

        publisher.publishEvent(new ProfileAddEvent(id, parts));

        OrderCreateEvent kafkaEvent = OrderCreateEvent.builder()
                .clientId(id)
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
// тут остановился
    @Override
    @Transactional
    @Caching(
            put = {
                    @CachePut(
                            cacheNames = CacheKeys.ORDER_KEY_PREFIX,
                            key = "#result.id()"
                    ),
                    @CachePut(
                            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
                            key = "#result.clientId()"
                    )
            }
    )
    public OrderDTO updateOrderByOrderId(Long id, OrderDTO orderDTO) {
        log.info("Called updateOrderByOrderId with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));

        order.setTotalAmount(orderDTO.totalAmount());
        order.setStatus(orderDTO.status());
        order.setOrderDate(orderDTO.orderDate());

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

            log.info("Keys: {}, {}, {}, {} was incremented. Keys: {}, {} was invalidated.",
                    CacheVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,
                    CacheKeys.CLIENT_KEY_PREFIX,
                    CacheKeys.ORDER_KEY_PREFIX
            );
        });

        return mapperService.orderToDTO(order);
    }

    @Override
    @Transactional
    @Caching(
            evict = {
                    @CacheEvict(
                            cacheNames = CacheKeys.ORDER_KEY_PREFIX,
                            key = "#id",
                            condition = "#id > 0"
                    ),
                    @CacheEvict(
                            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
                            allEntries = true
                    )
            }
    )
    public void deleteOrderByOrderId(Long id) {
        log.info("Called deleteOrderById with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));

        Client client = order.getClient();
        if (client != null) {
            client.getOrders().remove(order);
        }

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

            log.info("Keys: {}, {}, {}, {} was incremented. Keys: {}, {} was invalidated.",
                    CacheVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,
                    CacheKeys.CLIENT_KEY_PREFIX,
                    CacheKeys.ORDER_KEY_PREFIX
            );
        });

        orderRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.ORDERS_FILTER_STATUS_KEY_PREFIX,
            keyGenerator = "filterStatusKeyGen",
            condition = "#filter.status() != null && #filter.fromDate() == null" +
                    "&& #filter.toDate() == null && #filter.minAmount() == null" +
                    "&& #filter.maxAmount() == null"
    )
    public OrdersPageDTO getOrdersByFilters(OrderFilterDTO filter, int page, int size) {
        log.info("Called getOrdersByFilters with filter: {}", filter);

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "id"));

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null) {
                predicates.add(cb.equal((root.get("status")), filter.status()));
            }

            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), filter.fromDate()));
            }

            if (filter.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), filter.toDate()));
            }

            if (filter.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), filter.minAmount()));
            }

            if (filter.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), filter.maxAmount()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<OrderDTO> result = orderRepository.findAll(spec, pageable)
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
}
