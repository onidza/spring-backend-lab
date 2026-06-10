package com.onidza.backend.service.client;

import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.dto.client.ClientsUpdateDTO;

public interface ClientService {

    ClientDTO getClientById(Long id);

    ClientsPageDTO getClientsPage(int page, int size);

    ClientDTO addClient(ClientDTO clientDTO);

    ClientDTO updateClientById(Long id, ClientsUpdateDTO clientDTO);

    void deleteClientById(Long id);
}
