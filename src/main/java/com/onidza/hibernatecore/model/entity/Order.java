package com.onidza.hibernatecore.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String status;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    public Order(LocalDateTime orderDate, BigDecimal totalAmount, String status) {
        this.orderDate = orderDate != null ? orderDate : LocalDateTime.now();
        this.totalAmount = totalAmount;
        this.status = status;
    }
}
