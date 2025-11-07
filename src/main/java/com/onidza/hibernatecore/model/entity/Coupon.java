package com.onidza.hibernatecore.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private final String code;

    @Column(name = "discount")
    private final float discount;

    @Column(name = "expiration_date")
    private final LocalDateTime expirationDate;

    @ManyToMany(mappedBy = "coupons")
    private List<Client> clients = new ArrayList<>();

    public Coupon(String code, float discount, LocalDateTime expirationDate) {
        this.code = code;
        this.discount = discount;
        this.expirationDate = expirationDate;
    }

    public List<Client> getClients() {
        return Collections.unmodifiableList(clients);
    }
}
