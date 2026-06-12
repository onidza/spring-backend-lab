package com.onidza.backend.util.filters;

import com.onidza.backend.model.dto.enums.OrderStatus;
import com.onidza.backend.model.entity.Order;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OrderSpecification {

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> orderDateFrom(LocalDateTime fromDate) {
        return (root, query, cb) ->
                fromDate == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("orderDate"), fromDate);
    }

    public static Specification<Order> orderDateTo(LocalDateTime toDate) {
        return (root, query, cb) ->
                toDate == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("orderDate"), toDate);
    }

    public static Specification<Order> minAmount(BigDecimal minAmount) {
        return (root, query, cb) ->
                minAmount == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount);
    }

    public static Specification<Order> maxAmount(BigDecimal maxAmount) {
        return (root, query, cb) ->
                maxAmount == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("totalAmount"), maxAmount);
    }
}
}
