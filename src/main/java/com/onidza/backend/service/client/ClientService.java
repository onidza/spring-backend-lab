package com.onidza.backend.service.client;

import com.onidza.backend.model.dto.ClientDTO;

import java.util.List;

public interface ClientService {

    ClientDTO getClientById(Long id);

    List<ClientDTO> getAllClients();

    ClientDTO addClient(ClientDTO clientDTO);

    ClientDTO updateClient(Long id, ClientDTO clientDTO);

    void deleteClient(Long id);
}
