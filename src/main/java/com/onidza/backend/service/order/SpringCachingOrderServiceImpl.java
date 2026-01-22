package com.onidza.backend.service.order;

import com.onidza.backend.config.CacheKeys;
import com.onidza.backend.config.CacheVersionKeys;
import com.onidza.backend.config.CacheVersionService;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.dto.order.OrderFilterDTO;
import com.onidza.backend.model.dto.order.OrdersPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.OrderRepository;
import com.onidza.backend.service.TransactionAfterCommitExecutor;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpringCachingOrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final TransactionAfterCommitExecutor afterCommitExecutor;
    private final CacheVersionService versionService;

    private static final String ORDER_NOT_FOUND = "Order not found";
    private static final String CLIENT_NOT_FOUND = "Client not found";

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.ORDER_KEY_PREFIX,
            key = "'id:' + #id",
            condition = "#id > 0"
    )
    public OrderDTO getOrderById(Long id) {
        log.info("Called getOrderById with id: {}", id);

        return mapperService.orderToDTO(orderRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, ORDER_NOT_FOUND)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheKeys.ORDERS_PAGE_VER_KEY,
            keyGenerator = "orderPageKeyGen"
    )
    public OrdersPageDTO getOrdersPage(int page, int size) {
        log.info("Called getOrdersPage");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
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
            cacheNames = CacheKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
            keyGenerator = "orderPageByClientIdKeyGen"
    )
    public OrdersPageDTO getOrdersPageByClientId(Long id, int page, int size) {
        log.info("Called getOrdersPageByClientId with id: {}", id);

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
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
    @CacheEvict(
            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
            key = "'id:' + #id",
            condition = "#id > 0"
    )
    public OrderDTO addOrderToClient(Long id, OrderDTO orderDTO) {
        log.info("Called addOrderToClient with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND));

        Order order = mapperService.orderDTOToEntity(orderDTO);
        order.setClient(client);
        client.getOrders().add(order);

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
            versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_PREFIX);

            log.info("Keys: {}, {}, {} was incremented. Key {} was invalidated.",
                    CacheVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_PREFIX,
                    CacheVersionKeys.CLIENT_KEY_PREFIX
            );
        });

        return mapperService.orderToDTO(orderRepository.save(order));
    }

    @Override
    @Transactional
    @Caching(
            put = {
                    @CachePut(
                            cacheNames = CacheKeys.ORDER_KEY_PREFIX,
                            key = "'id:' + #result.id()",
                            condition = "#id > 0"
                    ),
                    @CachePut(
                            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
                            key = "'id:' + #result.clientId()"
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
            versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_PREFIX);

            log.info("Keys: {}, {}, {}, {} was incremented. Keys: {}, {} was invalidated.",
                    CacheVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_PREFIX,
                    CacheVersionKeys.CLIENT_KEY_PREFIX,
                    CacheVersionKeys.ORDER_KEY_PREFIX
            );
        });

        return mapperService.orderToDTO(order);
    }

    @Override
    @Transactional
    @Caching(
            put = {
                    @CachePut(
                            cacheNames = CacheKeys.ORDER_KEY_PREFIX,
                            key = "'id:' + #result.id()",
                            condition = "#id > 0"
                    ),
                    @CachePut(
                            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
                            key = "'id:' + #result.clientId()"
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
            versionService.bumpVersion(CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_PREFIX);

            log.info("Keys: {}, {}, {}, {} was incremented. Keys: {}, {} was invalidated.",
                    CacheVersionKeys.ORDERS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_PAGE_BY_CLIENT_ID_VER_KEY,
                    CacheVersionKeys.CLIENTS_PAGE_VER_KEY,
                    CacheVersionKeys.ORDERS_FILTER_STATUS_KEY_PREFIX,
                    CacheVersionKeys.CLIENT_KEY_PREFIX,
                    CacheVersionKeys.ORDER_KEY_PREFIX
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
    public List<OrderDTO> getOrdersByFilters(OrderFilterDTO filter) {
        log.info("Called getOrdersByFilters with filter: {}", filter);

        List<Order> orders = orderRepository.findAll((root, query, cb) -> {
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
        });

        return orders.stream()
                .map(mapperService::orderToDTO)
                .toList();
    }
}
