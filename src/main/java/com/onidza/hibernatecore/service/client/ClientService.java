package com.onidza.hibernatecore.service.client;

import com.onidza.hibernatecore.model.dto.ClientDTO;

import java.util.List;

public interface ClientService {

    ClientDTO getClientById(Long id);

    List<ClientDTO> getAllClients();

    ClientDTO addClient(ClientDTO clientDTO);

    ClientDTO updateClient(Long id, ClientDTO clientDTO);

    void deleteClient(Long id);
}
