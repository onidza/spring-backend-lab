package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Profile;
import com.onidza.hibernatecore.repository.ClientRepository;
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
class ClientServiceIntegrationTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void getAllClients_returnsAllClientsWithRelations() {
        Profile profile1 = new Profile("Voronezh, d.123", "8(904)084-47-07");
        Client client1 = new Client("Ivan", "ivan@mail.com", profile1);

        Profile profile2 = new Profile("Moscow, d.99", "8(915)999-88-77");
        Client client2 = new Client("Anna", "anna@mail.com", profile2);

        clientRepository.save(client1);
        clientRepository.save(client2);

        List<ClientDTO> clients = clientService.getAllClients();

        Assertions.assertEquals(2, clients.size());

        ClientDTO first = clients.get(0);

        Assertions.assertNotNull(first.profile());
        Assertions.assertNotNull(first.orders());
        Assertions.assertNotNull(first.coupons());

        Assertions.assertEquals("Ivan", first.name());
        Assertions.assertEquals("Voronezh, d.123", first.profile().address());

        ClientDTO second = clients.get(1);
        Assertions.assertEquals("Anna", second.name());
        Assertions.assertEquals("Moscow, d.99", second.profile().address());

        Assertions.assertTrue(first.orders().isEmpty());
        Assertions.assertTrue(first.coupons().isEmpty());
    }
}