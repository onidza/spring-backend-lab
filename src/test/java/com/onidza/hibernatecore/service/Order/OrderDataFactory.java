package com.onidza.hibernatecore.service.Order;

import com.onidza.hibernatecore.model.OrderStatus;
import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.model.dto.order.OrderDTO;
import com.onidza.hibernatecore.model.dto.order.OrderFilterDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderDataFactory {

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
                                LocalDateTime.of(2020, 1, 1, 12, 0),
                                new BigDecimal("1500"),
                                OrderStatus.NEW,
                                null)
                )),

                Collections.emptyList()
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
                                LocalDateTime.of(2025, 1, 1, 12, 0),
                                new BigDecimal("99"),
                                OrderStatus.CANCELLED,
                                null)
                )),

                Collections.emptyList()
        );
    }

    static OrderFilterDTO createFilter() {
        return new OrderFilterDTO(
                OrderStatus.NEW,
                LocalDateTime.of(2019, 1, 1, 12, 0),
                LocalDateTime.of(2021, 1, 1, 12, 0),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(999999)
        );
    }

    static OrderDTO createOrderDTOForUpdate() {
        return new OrderDTO(
                null,
                LocalDateTime.of(2025, 1, 1, 12, 0),
                new BigDecimal(99),
                OrderStatus.CANCELLED,
                null
        );
    }

    static OrderDTO createOrderDTOAfterUpdate() {
        return new OrderDTO(
                1L,
                LocalDateTime.of(2025, 1, 1, 12, 0),
                new BigDecimal(99),
                OrderStatus.CANCELLED,
                1L
        );
    }

    static ClientDTO createInputClientDTOWithEmptyOrders() {
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

    static Order createPersistentOrderEntity() {
        Order persistentOrder = new Order(
                LocalDateTime.of(2020, 1, 1, 12, 0),
                new BigDecimal("1500"),
                OrderStatus.NEW
        );

        persistentOrder.setId(1L);
        return persistentOrder;
    }

    static Order createDistinctPersistentOrderEntity() {
        Order persistentOrder = new Order(
                LocalDateTime.of(2021, 1, 1, 12, 0),
                new BigDecimal("99"),
                OrderStatus.CANCELLED
        );

        persistentOrder.setId(2L);
        return persistentOrder;
    }

    static Order createPersistentOrderEntityForAdd() {
        Order persistentOrder = new Order(
                LocalDateTime.of(2025, 1, 1, 12, 0),
                new BigDecimal("99"),
                OrderStatus.CANCELLED
        );

        persistentOrder.setId(1L);
        return persistentOrder;
    }

    static OrderDTO createPersistentOrderDTO() {
        return new OrderDTO(
                1L,
                LocalDateTime.of(2020, 1, 1, 12, 0),
                new BigDecimal("1500"),
                OrderStatus.NEW,
                null
        );
    }

    static OrderDTO createDistinctPersistentOrderDTO() {
        return new OrderDTO(
                2L,
                LocalDateTime.of(2021, 1, 1, 12, 0),
                new BigDecimal("99"),
                OrderStatus.CANCELLED,
                null
        );
    }

    static Client createPersistClientWithOrders() {
        Client client = new Client(
                "Ivan",
                "ivan-st233@mail.ru",
                null
        );
        client.setId(1L);

        Order firstOrder = createPersistentOrderEntity();
        Order secondOrder = createDistinctPersistentOrderEntity();
        firstOrder.setClient(client);
        secondOrder.setClient(client);

        client.getOrders().add(firstOrder);
        client.getOrders().add(secondOrder);

        return client;
    }


    static Client createPersistClientEntityWithEmptyOrders() {
        Client client = new Client(
                "Ivan",
                "ivan-st233@mail.ru",
                null
        );

        client.setId(1L);
        return client;
    }
}
