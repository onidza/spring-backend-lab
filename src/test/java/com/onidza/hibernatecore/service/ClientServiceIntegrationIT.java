package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
        ClientDTO secondInputClientDTO = ClientDataFactory.createSecondInputClientDTO();

        clientService.addClient(firstInputClientDTO);
        clientService.addClient(secondInputClientDTO);

        List<ClientDTO> clients = clientService.getAllClients();

        Assertions.assertEquals(2, clients.size());

        Assertions.assertTrue(clients.stream().anyMatch(c -> c.name().equals("Ivan")));
        Assertions.assertTrue(clients.stream().anyMatch(c -> c.name().equals("Sasha")));

        clients.forEach(c -> {
            Assertions.assertNotNull(c.profile(), "Profile should not be null");
            Assertions.assertNotNull(c.orders(), "Orders list should not be null");
            Assertions.assertNotNull(c.coupons(), "Coupons list should not be null");
            Assertions.assertTrue(c.orders().isEmpty(), "Orders should be empty initially");
            Assertions.assertTrue(c.coupons().isEmpty(), "Coupons should be empty initially");
        });

        List<Long> ids = clients.stream()
                .map(ClientDTO::id)
                .toList();

        Assertions.assertEquals(2, ids.stream().distinct().count());
    }
}