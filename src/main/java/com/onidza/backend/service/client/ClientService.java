package com.onidza.backend.service.client;

import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.dto.client.ClientsUpdateDTO;

public interface ClientService {

    ClientDTO getClient(Long id);

    ClientsPageDTO getClientsPage(int page, int size);

    ClientDTO createClient(ClientDTO clientDTO);

    ClientDTO updateClient(Long id, ClientsUpdateDTO clientDTO);

    void deleteClient(Long id);
}
