package com.onidza.backend.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.backend.config.cache.manual.CacheManualKeys;
import com.onidza.backend.config.cache.manual.CacheManualVersionKeys;
import com.onidza.backend.config.cache.manual.CacheTtlProps;
import com.onidza.backend.config.cache.CacheVersionService;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class ManualOrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final CacheTtlProps ttlProps;
    private final CacheVersionService versionService;
    private final TransactionAfterCommitExecutor afterCommitExecutor;

    private final MeterRegistry meterRegistry;
    private Counter filterCalls;
    private Timer filterTimer;
    private Counter filterCacheHits;
    private Counter filterCacheMisses;

    private static final String ORDER_NOT_FOUND = "Order not found";
    private static final String CLIENT_NOT_FOUND = "Client not found";

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

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        log.info("Called getOrderById with id: {}", id);

        Object objFromCache = redisTemplate.opsForValue().get(CacheManualKeys.ORDER_KEY_PREFIX + id);
        if (objFromCache != null) {
            log.info("Returned order from cache with id: {}", id);
            return objectMapper.convertValue(objFromCache, OrderDTO.class);
        }

        OrderDTO existing = mapperService.orderToDTO(orderRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND)));

        redisTemplate.opsForValue().set(CacheManualKeys.ORDER_KEY_PREFIX + id, existing,
                ttlProps.getOrderById());
        log.info("getOrderById was cached...");

        log.info("Returned order from db with id: {}", id);
        return existing;
    }

    @Override
    @Transactional(readOnly = true)
    public OrdersPageDTO getOrdersPage(int page, int size) {
        log.info("Called getOrdersPage");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        long ver = versionService.getKeyVersion(CacheManualVersionKeys.ORDERS_PAGE_VER_KEY);
        String key = CacheManualKeys.ORDERS_PAGE_PREFIX + ver + ":p=" + safePage + ":s=" + safeSize;


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

        Page<OrderDTO> result = orderRepository.findAll(pageable)
                .map(mapperService::orderToDTO);

        OrdersPageDTO response = new OrdersPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        redisTemplate.opsForValue().set(key, response, ttlProps.getOrdersPage());
        log.info("getOrdersPage was cached...");

        log.info("Returned page from db with size={}", response.items().size());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrdersPageDTO getOrdersPageByClientId(Long id, int page, int size) {
        log.info("Called getOrdersPageByClientId with id: {}", id);

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        long ver = versionService.getKeyVersion(CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
        String key = CacheManualKeys.ORDERS_PAGE_BY_CLIENT_ID_PREFIX
                + id + ":ver=" + ver + ":p=" + safePage + ":s=" + safeSize;

        Object objFromCache = redisTemplate.opsForValue().get(key);
        if (objFromCache != null) {
            OrdersPageDTO cached = objectMapper.convertValue(objFromCache, OrdersPageDTO.class);

            log.info("Returned page from cache with size={}", cached.items().size());
            return cached;
        }

        if (!clientRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by("id").ascending());

        Page<OrderDTO> result = orderRepository.findByClientId(id, pageable)
                .map(mapperService::orderToDTO);

        OrdersPageDTO response = new OrdersPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );

        redisTemplate.opsForValue().set(key, response, ttlProps.getOrdersPageByClientId());
        log.info("getOrdersPageByClientId was cached...");

        log.info("Returned ordersListByClientId: {} from db with size: {}", id, result.getContent().size());
        return response;
    }

    @Override
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

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

            redisTemplate.delete(CacheManualKeys.CLIENT_KEY_PREFIX + clientId);
            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);

            log.info("Keys: {}, {}, {}, {} was incremented. Key {} was invalidated.",
                    CacheManualVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,

                    CacheManualKeys.CLIENT_KEY_PREFIX + clientId
            );
        });

        return mapperService.orderToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDTO updateOrderByOrderId(Long id, OrderDTO orderDTO) {
        log.info("Called updateOrderByOrderId with id: {}", id);

        Order order = orderRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND));

        Long clientId = order.getClient().getId();

        order.setTotalAmount(orderDTO.totalAmount());
        order.setStatus(orderDTO.status());
        order.setOrderDate(orderDTO.orderDate());

        Order saved = orderRepository.save(order);

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CacheManualKeys.ORDER_KEY_PREFIX + id);
            versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

            redisTemplate.delete(CacheManualKeys.CLIENT_KEY_PREFIX + clientId);
            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);

            log.info("Keys: {}, {}, {}, {} was incremented. Keys {}, {} was invalidated.",
                    CacheManualVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,

                    CacheManualKeys.ORDER_KEY_PREFIX + id,
                    CacheManualKeys.CLIENT_KEY_PREFIX + clientId
            );
        });

        return mapperService.orderToDTO(saved);
    }

    @Override
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

        afterCommitExecutor.run(() -> {
            redisTemplate.delete(CacheManualKeys.ORDER_KEY_PREFIX + id);
            versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);

            redisTemplate.delete(CacheManualKeys.CLIENT_KEY_PREFIX + clientId);
            versionService.bumpVersion(CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY);

            log.info("Keys: {}, {}, {}, {} was incremented. Keys {}, {} was invalidated.",
                    CacheManualVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheManualVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER,
                    CacheManualVersionKeys.CLIENTS_PAGE_VER_KEY,

                    CacheManualKeys.ORDER_KEY_PREFIX + id,
                    CacheManualKeys.CLIENT_KEY_PREFIX + clientId
            );
        });

        orderRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdersPageDTO getOrdersByFilters(OrderFilterDTO filter, int page, int size) {
        log.info("Called getOrdersByFilters with filter: {}", filter);

        filterCalls.increment();

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "orderDate")
        );

        boolean cacheable = isCacheableStatusOnly(filter);
        if (cacheable) {
            long ver = versionService.getKeyVersion(CacheManualVersionKeys.ORDERS_FILTER_STATUS_KEY_VER);
            var key = CacheManualKeys.ORDERS_FILTER_STATUS_PREFIX
                    + ":status=" + filter.status().name()
                    + ":page=" + safePage
                    + ":size=" + safeSize
                    + ":sort=orderDate,DESC"
                    + ":ver=" + ver;

            Object objFromCache = redisTemplate.opsForValue().get(key);

            if (objFromCache != null) {
                OrdersPageDTO cached = objectMapper.convertValue(objFromCache, OrdersPageDTO.class);

                filterCacheHits.increment();

                log.info("Returned getOrdersByFilters with status: {} from cache", filter.status().name());
                return cached;
            }

            filterCacheMisses.increment();

            OrdersPageDTO computed = Objects.requireNonNull(filterTimer.record(() -> {
                Specification<Order> spec = (root, query, cb) ->
                        cb.equal(root.get("status"), filter.status());

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
            }));

            redisTemplate.opsForValue().set(key, computed, ttlProps.getOrdersByFilters());
            log.info("Cached orders by status: key={}, size={} ...", key, computed.items().size());
            return computed;
        }

        return filterTimer.record(() -> {
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
