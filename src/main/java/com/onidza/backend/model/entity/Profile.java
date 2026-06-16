package com.onidza.backend.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "profile_seq")
    @SequenceGenerator(
            name = "profile_seq",
            sequenceName = "profile_seq"
    )
    private Long id;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "phone", unique = true, nullable = false)
    private String phone;

    @OneToOne(mappedBy = "profile")
    private Client client;

    public Profile(String address, String phone) {
        this.address = address;
        this.phone = phone;
    }

    public void updateInfo(String address, String phone) {
        this.address = address;
        this.phone = phone;
    }
}
