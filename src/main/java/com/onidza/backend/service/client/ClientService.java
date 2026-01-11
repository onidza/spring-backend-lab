package com.onidza.backend.service.client;

import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;

public interface ClientService {

    ClientDTO getClientById(Long id);

    ClientsPageDTO getAllClientsPage(int page, int size);

    ClientDTO addClient(ClientDTO clientDTO);

    ClientDTO updateClient(Long id, ClientDTO clientDTO);

    void deleteClient(Long id);
}
