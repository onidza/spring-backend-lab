package com.onidza.backend.service.client;

import com.onidza.backend.model.dto.ClientDTO;
import org.springframework.data.domain.Page;

public interface ClientService {

    ClientDTO getClientById(Long id);

    Page<ClientDTO> getAllClientsPage(int page, int size);

    ClientDTO addClient(ClientDTO clientDTO);

    ClientDTO updateClient(Long id, ClientDTO clientDTO);

    void deleteClient(Long id);
}
