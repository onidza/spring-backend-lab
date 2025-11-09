package com.onidza.hibernatecore.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "discount", nullable = false)
    private float discount;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @ManyToMany(mappedBy = "coupons")
    private List<Client> clients = new ArrayList<>();

    public Coupon(String code, float discount, LocalDateTime expirationDate) {
        this.code = code;
        this.discount = discount;
        this.expirationDate = expirationDate;
    }
}
