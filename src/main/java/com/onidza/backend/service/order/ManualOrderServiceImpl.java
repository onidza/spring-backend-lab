package com.onidza.backend.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.OrderRepository;
import com.onidza.backend.service.TransactionAfterCommitExecutor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class ManualOrderServiceImpl implements OrderService {

    private final MeterRegistry meterRegistry;

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    private Counter filterCalls;
    private Timer filterTimer;

    private Counter filterCacheHits;
    private Counter filterCacheMisses;

    private static final String ORDER_NOT_FOUND = "Order not found";
    private static final String CLIENT_NOT_FOUND = "Client not found";

    private static final String ORDER_KEY_PREFIX = "order:";
    private static final Duration ORDER_TTL = Duration.ofMinutes(10);

    private static final String PAGE_ORDERS_KEY = "orders:p=";
    private static final Duration PAGE_ORDERS_TTL = Duration.ofMinutes(10);

    private static final String ALL_ORDERS_BY_CLIENT_ID_KEY_PREFIX = "orders:byClientId:v1:";
    private static final Duration ALL_ORDERS_BY_CLIENT_ID_TTL = Duration.ofMinutes(10);

    private static final String ORDERS_FILTER_STATUS_KEY_PREFIX = "orders:filter:status:v1:";
    private static final Duration ORDERS_FILTER_STATUS_TTL = Duration.ofSeconds(30);

    private static final String CLIENT_KEY_PREFIX = "client:";
    private static final String ALL_CLIENTS_KEY = "clients:all:v1";

    @PostConstruct
    void initMetrics() {
        String dynamicTag = "dynamic";
        String dynamicType = "type";

        this.filterCalls = Counter.builder("orders.filters.calls")
                .tag(dynamicType, dynamicTag)
                .register(meterRegistry);

        this.filterTimer = Timer.builder("orders.filters.latency")
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);

        this.filterCacheHits = Counter.builder("orders.filters.cache.hits")
                .tag(dynamicType, dynamicTag)
                .register(meterRegistry);

        this.filterCacheMisses = Counter.builder("orders.filters.cache.misses")
                .tag(dynamicType, dynamicTag)
                .register(meterRegistry);

        log.info("Order metrics initialized");
    }

    public OrderDTO getOrderById(Long id) {
        log.info("Called getOrderById with id: {}", id);

        Object objFromCache = redisTemplate.opsForValue().get(ORDER_KEY_PREFIX + id);
        if (objFromCache != null) {
            log.info("Returned order from cache with id: {}", id);
            return objectMapper.convertValue(objFromCache, OrderDTO.class);
        }

        OrderDTO existing = mapperService.orderToDTO(orderRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND)));

        redisTemplate.opsForValue().set(ORDER_KEY_PREFIX + id, existing, ORDER_TTL);
        log.info("getOrderById was cached...");

        log.info("Returned order from db with id: {}", id);
        return existing;
    }

    public OrdersPageDTO getOrdersPage(int page, int size) {
        log.info("Called getOrdersPage");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        String key = PAGE_ORDERS_KEY + safePage + ":s=" + safeSize;


        Object objFromCache = redisTemplate.opsForValue().get(key);
        if (objFromCache != null) {
            OrdersPageDTO cached = objectMapper.convertValue(objFromCache, OrdersPageDTO.class);

            log.info("Returned page from cache with size={}", cached.items().size());
            return cached;
        }

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("id").ascending());

        Page<OrderDTO> result = orderRepository.findAll(pageable).map(mapperService::orderToDTO);

        OrdersPageDTO response = new OrdersPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        redisTemplate.opsForValue().set(key, response, PAGE_ORDERS_TTL);
        log.info("getOrdersPage was cached...");

        log.info("Returned page from db with size={}", response.items().size());
        return response;
    }

    public List<OrderDTO> getAllOrdersByClientId(Long id) {
        log.info("Called getAllOrdersByClientId with id: {}", id);

        Object objFromCache = redisTemplate.opsForValue().get(ALL_ORDERS_BY_CLIENT_ID_KEY_PREFIX + id);
        if (objFromCache instanceof List<?> raw) {
            List<OrderDTO> orderDTOList = raw.stream()
                    .map(o -> objectMapper.convertValue(o, OrderDTO.class))
                    .toList();

            log.info("Returned ordersDtoListById from cache with size={}", orderDTOList.size());
            return orderDTOList;
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        List<OrderDTO> orderDTOList = client.getOrders()
                .stream()
                .map(mapperService::orderToDTO)
                .toList();

        redisTemplate.opsForValue().set(ALL_ORDERS_BY_CLIENT_ID_KEY_PREFIX + id, orderDTOList, ALL_ORDERS_BY_CLIENT_ID_TTL);
        log.info("getAllOrdersByClientId was cached...");

        log.info("Returned ordersListByClientId: {} from db with size: {}", id, orderDTOList.size());
        return orderDTOList;
    }

    @Transactional
    public OrderDTO updateOrderByOrderId(Long id, OrderDTO orderDTO) {
        log.info("Called updateOrderByOrderId with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));

        Long clientId = order.getClient().getId();
        String oldStatus = order.getStatus().name();
        String newStatus = orderDTO.status().name();

        order.setTotalAmount(orderDTO.totalAmount());
        order.setStatus(orderDTO.status());
        order.setOrderDate(orderDTO.orderDate());

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(ORDER_KEY_PREFIX + id);
            redisTemplate.delete((PAGE_ORDERS_KEY));
            redisTemplate.delete(ALL_ORDERS_BY_CLIENT_ID_KEY_PREFIX + clientId);

            redisTemplate.delete(ORDERS_FILTER_STATUS_KEY_PREFIX + newStatus);
            redisTemplate.delete(ORDERS_FILTER_STATUS_KEY_PREFIX + oldStatus);

            redisTemplate.delete(CLIENT_KEY_PREFIX + clientId);
            redisTemplate.delete(ALL_CLIENTS_KEY);

        });

        return mapperService.orderToDTO(order);
    }

    @Transactional
    public OrderDTO addOrderToClient(Long id, OrderDTO orderDTO) {
        log.info("Called addOrderToClient with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        Order order = mapperService.orderDTOToEntity(orderDTO);
        order.setClient(client);
        client.getOrders().add(order);

        Long clientId = client.getId();
        String status = order.getStatus().name();

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(PAGE_ORDERS_KEY);
            redisTemplate.delete(ALL_ORDERS_BY_CLIENT_ID_KEY_PREFIX + clientId);

            redisTemplate.delete(ORDERS_FILTER_STATUS_KEY_PREFIX + status);

            redisTemplate.delete(CLIENT_KEY_PREFIX + clientId);
            redisTemplate.delete(ALL_CLIENTS_KEY);
        });

        return mapperService.orderToDTO(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrderByOrderId(Long id) {
        log.info("Called deleteOrderByOrderId with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));

        Client client = order.getClient();
        if (client == null) {
            throw new IllegalStateException("Order has no client");
        }
        client.getOrders().remove(order);

        Long clientId = client.getId();
        String status = order.getStatus().name();

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(ORDER_KEY_PREFIX + id);
            redisTemplate.delete(PAGE_ORDERS_KEY);
            redisTemplate.delete(ALL_ORDERS_BY_CLIENT_ID_KEY_PREFIX + clientId);

            redisTemplate.delete(ORDERS_FILTER_STATUS_KEY_PREFIX + status);

            redisTemplate.delete(CLIENT_KEY_PREFIX + clientId);
            redisTemplate.delete(ALL_CLIENTS_KEY);
        });

        orderRepository.deleteById(id);
    }

    public List<OrderDTO> getOrdersByFilters(OrderFilterDTO filter) {
        log.info("Called getOrdersByFilters with filter: {}", filter);

        filterCalls.increment();

        boolean cacheable = isCacheableStatusOnly(filter);
        if (cacheable) {
            var key = ORDERS_FILTER_STATUS_KEY_PREFIX + filter.status().name();

            Object objFromCache = redisTemplate.opsForValue().get(key);

            if (objFromCache instanceof List<?> raw) {
                List<OrderDTO> orderDTOList = raw.stream()
                        .map(o -> objectMapper.convertValue(o, OrderDTO.class))
                        .toList();

                filterCacheHits.increment();

                log.info("Returned getOrdersByFilters with status: {} from cache", filter.status().name());
                return orderDTOList;
            }

            filterCacheMisses.increment();

            List<OrderDTO> dtoList = Objects.requireNonNull(filterTimer.record(() -> {
                List<Order> orders = orderRepository.findAll((root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(cb.equal(root.get("status"), filter.status()));
                    return cb.and(predicates.toArray(new Predicate[0]));
                });

                return orders.stream()
                        .map(mapperService::orderToDTO)
                        .toList();
            }));

            redisTemplate.opsForValue().set(key, dtoList, ORDERS_FILTER_STATUS_TTL);
            log.info("Cached orders by status: key={}, size={} ...", key, dtoList.size());
            return dtoList;
        }

        return filterTimer.record(() -> {
            List<Order> orders = orderRepository.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                //NULL already checked above in cache
                predicates.add(cb.equal((root.get("status")), filter.status())); //WHERE status = 'PAID'

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
            });

            return orders.stream()
                    .map(mapperService::orderToDTO)
                    .toList();
        });
    }

    private boolean isCacheableStatusOnly(OrderFilterDTO filter) {
        return filter.status() != null
                && filter.fromDate() == null
                && filter.toDate() == null
                && filter.minAmount() == null
                && filter.maxAmount() == null;
    }
}
