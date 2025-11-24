package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Profile;

import java.time.LocalDateTime;
import java.util.Collections;

public class ClientDataFactory {

    static ProfileDTO createPersistentProfileDTO() {
        return new ProfileDTO(
                1L,
                "Voronezh, d.123",
                "8(904)084-47-07",
                1L);
    }

    static Profile createPersistentProfileEntity() {
        Profile persistentProfile = new Profile("Voronezh, d.123", "8(904)084-47-07");
        persistentProfile.setId(1L);
        return persistentProfile;
    }

    static Client createPersistentClientEntity() {
        Client persistentClient = new Client("Ivan",
                "ivan-st233@mail.ru",
                createPersistentProfileEntity());

        persistentClient.setId(1L);
        persistentClient.setRegistrationDate(LocalDateTime.of(2020,1,1,12,0));
        persistentClient.setOrders(Collections.emptySet());
        persistentClient.setCoupons(Collections.emptySet());

        persistentClient.getProfile().setClient(persistentClient);

        return persistentClient;
    }

    static ClientDTO createPersistentClientDTO() {
        return new ClientDTO(
                1L,
                "Ivan",
                "ivan-st233@mail.ru",
                LocalDateTime.of(2020,1,1,12,0),

                createPersistentProfileDTO(),

                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    static ClientDTO createInputClientDTO() {
        return new ClientDTO(
                null,
                "Ivan",
                "ivan-st233@mail.ru",
                null,

                new ProfileDTO(
                        null,
                        "Voronezh, d.123",
                        "8(904)084-47-07",
                        null),

                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    static ClientDTO createSecondInputClientDTO() {
        return new ClientDTO(
                null,
                "Sasha",
                "sasha@mail.ru",
                null,

                new ProfileDTO(
                        null,
                        "Voronezh, d.130",
                        "8(904)777-77-77",
                        null),

                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    static Profile createInputProfileEntity() {
        return new Profile(
                "Voronezh, d.130",
                "8(904)777-77-77"
        );
    }

    static Client createInputClientEntity() {
        return new Client(
                "Ivan",
                "ivan-st233@mail.ru",
                createInputProfileEntity()

        );
    }

//    static Profile createProfile() {
//        return new Profile(
//                "Voronezh, d.123",
//                "8(904)084-47-07"
//        );
//    }
//
//    static CouponDTO createInputCouponDTO() {
//        return new CouponDTO(
//                null,
//                "DISCOUNT5%",
//                5.51f,
//                null,
//                null);
//    }
//
//    static Coupon createExpectedCoupon() {
//        return new Coupon(
//                "DISCOUNT5%",
//                5.51f,
//                LocalDateTime.now()
//        );
//    }

}
