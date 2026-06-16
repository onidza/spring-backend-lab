package com.onidza.backend.model.entity;

import com.onidza.backend.model.enums.OrderStatus;
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
    @SequenceGenerator(
            name = "order_seq",
            sequenceName = "order_seq"
    )
    private Long id;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    public Order(LocalDateTime orderDate, BigDecimal totalAmount, OrderStatus status) {
        this.orderDate = orderDate != null ? orderDate : LocalDateTime.now();
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public void setBiClientOrder(Client client) {
        this.client = client;
        client.getOrders().add(this);
    }

    public void removeOrderFromClient() {
        this.client.getOrders().remove(this);
    }

    public void updateOrder(Order order) {
        this.orderDate = order.orderDate != null ? order.orderDate : this.orderDate;
        this.totalAmount = order.totalAmount;
        this.status = order.status;
    }
}
