package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClientServiceIntegrationIT {

    @Autowired
    private ClientService clientService;

    @Test
    void getAllClients_returnsAllClientsDTOWithRelations() {
        ClientDTO firstInputClientDTO = ClientDataFactory.createInputClientDTO();
        ClientDTO secondInputClientDTO = ClientDataFactory.createDistinctInputClientDTO();

        clientService.addClient(firstInputClientDTO);
        clientService.addClient(secondInputClientDTO);

        List<ClientDTO> clients = clientService.getAllClients();

        Assertions.assertEquals(2, clients.size());

        Assertions.assertTrue(clients.stream().anyMatch(c -> c.name().equals("Ivan")));
        Assertions.assertTrue(clients.stream().anyMatch(c -> c.name().equals("Sasha")));

        clients.forEach(c -> {
            Assertions.assertNotNull(c.profile());
            Assertions.assertNotNull(c.orders());
            Assertions.assertNotNull(c.coupons());

            Assertions.assertTrue(c.orders().stream()
                    .anyMatch(order ->
                            order.totalAmount().equals(new BigDecimal("11111"))
                            || order.totalAmount().equals(new BigDecimal("16445"))));

            Assertions.assertTrue(c.coupons().stream()
                    .anyMatch(co -> co.discount() == 8.8f || co.discount() == 5.0f));

        });

        List<Long> ids = clients.stream()
                .map(ClientDTO::id)
                .toList();

        Assertions.assertEquals(2, ids.stream().distinct().count());
    }
}