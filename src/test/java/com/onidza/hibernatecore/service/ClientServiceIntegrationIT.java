package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.OrderStatus;
import com.onidza.hibernatecore.model.dto.ClientDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClientServiceIntegrationIT {

    @Autowired
    private ClientService clientService;

    @Test
    void getClientById_returnClientDTOWithRelations() {
        ClientDTO inputClientDTO = ClientDataFactory.createInputClientDTO();

        ClientDTO saved = clientService.addClient(inputClientDTO);
        ClientDTO existing = clientService.getClientById(saved.id());

        Assertions.assertEquals(saved.id(), existing.id());
        Assertions.assertEquals("Ivan", existing.name());

        Assertions.assertNotNull(existing.profile());
        Assertions.assertNotNull(existing.coupons());
        Assertions.assertNotNull(existing.orders());

        Assertions.assertEquals("Voronezh, d.123", existing.profile().address());
        Assertions.assertEquals(1, existing.coupons().size());
        Assertions.assertEquals(saved.id(), existing.orders().get(0).clientId());
    }

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

        List<Long> ids = clients.stream().map(ClientDTO::id).toList();
        Assertions.assertEquals(2, ids.stream().distinct().count());
    }

    @Test
    void addClient_returnClientDTOWithRelations() {
        ClientDTO inputClientDTO = ClientDataFactory.createInputClientDTO();

        ClientDTO saved = clientService.addClient(inputClientDTO);

        Assertions.assertNotNull(saved.id());
        Assertions.assertEquals("Ivan", saved.name());

        Assertions.assertNotNull(saved.profile());
        Assertions.assertNotNull(saved.coupons());
        Assertions.assertNotNull(saved.orders());

        Assertions.assertEquals("Voronezh, d.123", saved.profile().address());
        Assertions.assertEquals(1, saved.coupons().size());
        Assertions.assertEquals(saved.id(), saved.orders().get(0).clientId());
    }

    @Test
    void updateClient_returnClientDTOWithRelations() {
        ClientDTO inputClientDTO = ClientDataFactory.createInputClientDTO();
        ClientDTO distinctInputClientDTO = ClientDataFactory.createDistinctInputClientDTO();

        ClientDTO saved = clientService.addClient(inputClientDTO);
        ClientDTO updated = clientService.updateClient(saved.id(),
                distinctInputClientDTO);

        Assertions.assertEquals(saved.id(), updated.id());
        Assertions.assertNotEquals(saved.name(), updated.name());
        Assertions.assertEquals("sasha@mail.ru", updated.email());

        Assertions.assertEquals("Moscow, d.1", updated.profile().address());
        Assertions.assertEquals(OrderStatus.NEW, updated.orders().get(0).status());
        Assertions.assertEquals("NEW CODE777", updated.coupons().get(0).code());

        Assertions.assertEquals(1, updated.coupons().size());
        Assertions.assertEquals(1, updated.orders().size());
    }

    @Test
    void deleteClient_returnNotingWithRelations() {
        ClientDTO inputClientDTO = ClientDataFactory.createInputClientDTO();

        ClientDTO saved = clientService.addClient(inputClientDTO);
        clientService.deleteClient(saved.id());

        Executable exec = () -> clientService.getClientById(saved.id());
        Assertions.assertThrows(ResponseStatusException.class, exec);

        List<ClientDTO> clients = clientService.getAllClients();
        Assertions.assertTrue(clients.stream().noneMatch(c -> c.id().equals(saved.id())));
    }
}