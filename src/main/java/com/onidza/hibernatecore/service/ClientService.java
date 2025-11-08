package com.onidza.hibernatecore.service;


import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.mapper.ClientMapper;
import com.onidza.hibernatecore.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
//    private final ClientMapper clientMapper;

    public ClientDTO getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        return clientMapper.toDTO(client);
    }

    public List<ClientDTO> getAllClients() {
        return clientRepository
                .findAll()
                .stream()
                .map(clientMapper::toDTO)
                .toList();
    }

    public Client addClient(ClientDTO clientDTO) {
        return clientRepository
                .save(clientMapper.toEntity(clientDTO));
    }

    public Client updateClient(Long id, ClientDTO updated) {
        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        Client client = clientMapper.toEntity(updated);
        existing.setName(client.getName());
        existing.setEmail(client.getEmail());
        existing.setProfile(client.getProfile());
        return clientRepository.save(existing);
    }

    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }
}
