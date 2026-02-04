package com.onidza.backend.service.Client;

import com.onidza.backend.model.dto.enums.OrderStatus;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.service.client.ClientServiceImpl;
import com.onidza.backend.service.testcontainers.AbstractITConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Transactional
class ClientServiceIntegrationIT extends AbstractITConfiguration {

    @Autowired
    private ClientServiceImpl clientServiceImpl;

    @Test
    void getClientById_returnClientDTOWithRelations() {
        ClientDTO inputClientDTO = ClientDataFactory.createInputClientDTO();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        ClientDTO existing = clientServiceImpl.getClientById(saved.id());

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
    void getAllClientsPage_returnsPageWithRelations() {
        ClientDTO firstInputClientDTO = ClientDataFactory.createInputClientDTO();
        ClientDTO secondInputClientDTO = ClientDataFactory.createDistinctInputClientDTO();

        clientServiceImpl.addClient(firstInputClientDTO);
        clientServiceImpl.addClient(secondInputClientDTO);

        ClientsPageDTO page = clientServiceImpl.getClientsPage(0, 20);

        Assertions.assertEquals(2, page.items().size());

        Assertions.assertTrue(page.items().stream().anyMatch(c -> c.name().equals("Ivan")));
        Assertions.assertTrue(page.items().stream().anyMatch(c -> c.name().equals("Sasha")));

        page.items().forEach(c -> {
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

        List<Long> ids = page.items().stream().map(ClientDTO::id).toList();
        Assertions.assertEquals(2, ids.stream().distinct().count());
    }

    @Test
    void addClient_returnClientDTOWithRelations() {
        ClientDTO inputClientDTO = ClientDataFactory.createInputClientDTO();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);

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

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        ClientDTO updated = clientServiceImpl.updateClient(saved.id(),
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

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        clientServiceImpl.deleteClient(saved.id());

        Executable exec = () -> clientServiceImpl.getClientById(saved.id());
        Assertions.assertThrows(ResponseStatusException.class, exec);

        ClientsPageDTO page = clientServiceImpl.getClientsPage(0, 20);
        Assertions.assertTrue(page.items().stream().noneMatch(c -> c.id().equals(saved.id())));
    }
}