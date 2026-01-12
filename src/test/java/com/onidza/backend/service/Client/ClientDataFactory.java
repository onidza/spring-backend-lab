package com.onidza.backend.service.Client;

import com.onidza.backend.model.OrderStatus;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class ClientDataFactory {

    static ProfileDTO createPersistentProfileDTO() {
        return new ProfileDTO(
                1L,
                "Voronezh, d.123",
                "8(904)084-47-07",
                1L);
    }

    static ClientDTO createPersistentClientDTO() {
        return new ClientDTO(
                1L,
                "Ivan",
                "ivan-st233@mail.ru",
                LocalDateTime.of(2020, 1, 1, 12, 0),

                createPersistentProfileDTO(),

                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    static Profile createPersistentProfileEntity() {
        Profile persistentProfile = new Profile(
                "Voronezh, d.123",
                "8(904)084-47-07");

        persistentProfile.setId(1L);
        return persistentProfile;
    }

    static Client createPersistentClientEntity() {
        Client persistentClient = new Client("Ivan",
                "ivan-st233@mail.ru",
                createPersistentProfileEntity());

        persistentClient.setId(1L);
        persistentClient.setRegistrationDate(LocalDateTime
                .of(2020, 1, 1, 12, 0));
        persistentClient.setOrders(new HashSet<>());
        persistentClient.setCoupons(new HashSet<>());
        persistentClient.getProfile().setClient(persistentClient);

        return persistentClient;
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

                new ArrayList<>(List.of(
                        new OrderDTO(null,
                                null,
                                new BigDecimal("11111"),
                                OrderStatus.PAID,
                                null)
                )),

                new ArrayList<>(List.of(
                        new CouponDTO(null,
                                "NEW CODE000000",
                                8.8f,
                                LocalDateTime.of(2020, 1, 1, 12, 0),
                                null)
                ))
        );
    }

    static ClientDTO createDistinctInputClientDTO() {
        return new ClientDTO(
                null,
                "Sasha",
                "sasha@mail.ru",
                null,

                new ProfileDTO(
                        null,
                        "Moscow, d.1",
                        "8(111)111-111-11",
                        null),

                new ArrayList<>(List.of(
                        new OrderDTO(null,
                                null,
                                new BigDecimal("16445"),
                                OrderStatus.NEW,
                                null)
                )),

                new ArrayList<>(List.of(
                        new CouponDTO(null,
                                "NEW CODE777",
                                5.0f,
                                LocalDateTime.of(2020, 1, 1, 12, 0),
                                null)
                ))
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
}
