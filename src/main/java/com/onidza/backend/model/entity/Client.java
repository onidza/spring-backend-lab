package com.onidza.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Generated
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
//    @JsonManagedReference("client-profile")
    private Profile profile;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference("client-order")
    private Set<Order> orders = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "client_coupons",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "coupon_id")
    )
//    @JsonManagedReference("client-coupon")
    private Set<Coupon> coupons = new HashSet<>();

    public Client(String name, String email, Profile profile) {
        this.name = name;
        this.email = email;
        this.registrationDate = LocalDateTime.now();
        this.profile = profile;
    }
}
