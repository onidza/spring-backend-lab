package com.onidza.backend.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "coupon_seq")
    @SequenceGenerator(
            name = "coupon_seq",
            sequenceName = "coupon_seq"
    )
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "discount", nullable = false)
    private float discount;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @ManyToMany(mappedBy = "coupons")
    private Set<Client> clients = new HashSet<>();

    public Coupon(String code, float discount, LocalDateTime expirationDate) {
        this.code = code;
        this.discount = discount;
        this.expirationDate = expirationDate;
    }

    public void updateCoupon(Coupon coupon) {
        this.code = coupon.code;
        this.discount = coupon.discount;
        this.expirationDate = coupon.expirationDate;
    }

    public void deleteCouponFromClients() {
        this.clients.forEach(client -> client.getCoupons().remove(this));
    }
}
