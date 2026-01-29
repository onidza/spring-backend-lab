package com.onidza.backend.model.entity;

import com.onidza.backend.model.dto.order.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Generated
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
//    @JsonBackReference("client-order")
    private Client client;

    public Order(LocalDateTime orderDate, BigDecimal totalAmount, OrderStatus status) {
        this.orderDate = orderDate != null ? orderDate : LocalDateTime.now();
        this.totalAmount = totalAmount;
        this.status = status;
    }
}
