package com.onidza.hibernatecore.service;


import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Profile;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final EntityManager entityManager;

    public ClientDTO getClientById(Long id) {
        log.info("Called getClientById with id: {}", id);

        return mapperService
                .clientToDTO(clientRepository.findById(id)
                        .orElseThrow(()
                                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found")));
    }

    public List<ClientDTO> getAllClients() {
        log.info("Called getAllClients");

        List<Client> clients = entityManager.createQuery(
                     """
                        SELECT DISTINCT c FROM Client c
                        LEFT JOIN FETCH c.profile
                        LEFT JOIN FETCH c.orders
                        LEFT JOIN FETCH c.coupons
                        """, Client.class
        ).getResultList();

        return clients.stream()
                .map(mapperService::clientToDTO)
                .toList();
    }

    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        log.info("Called addClient with name: {}", clientDTO.name());

        Client client = mapperService.clientDTOToEntity(clientDTO);

        if (client.getProfile() != null) {
            client.getProfile().setClient(client);
        }

        Client saved = clientRepository.save(client);
        return mapperService.clientToDTO(saved);
    }

    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        log.info("Called updateClient with id: {}", id);

        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        existing.setName(clientDTO.name());
        existing.setEmail(clientDTO.email());

        Profile existingProfile = existing.getProfile();
        if (existing.getProfile() != null && clientDTO.profile() != null) {
            existingProfile.setAddress(clientDTO.profile().address());
            existingProfile.setPhone(clientDTO.profile().phone());
        }

        if (clientDTO.coupons() != null) {
            existing.getCoupons().clear();
            clientDTO.coupons()
                    .stream()
                    .map(mapperService::couponDTOToEntity)
                    .forEach(coupon -> {
                        existing.getCoupons().add(coupon);
                        coupon.getClients().add(existing);
                    });
        }

        if (clientDTO.orders() != null) {
            existing.getOrders().clear();
            clientDTO.orders()
                    .stream()
                    .map(mapperService::orderDTOToEntity)
                    .forEach(order -> {
                        existing.getOrders().add(order);
                        order.setClient(existing);
                    });
        }

        return mapperService.clientToDTO(clientRepository.save(existing));
    }

    public void deleteClient(Long id) {
        log.info("Called deleteClient with id: {}", id);
        clientRepository.deleteById(id);
    }
}
