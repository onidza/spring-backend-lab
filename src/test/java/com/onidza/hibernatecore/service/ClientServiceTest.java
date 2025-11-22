package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.dto.ProfileDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Profile;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MapperService mapperService;

    @InjectMocks
    private ClientService clientService;


    @Test
    void getClientById_existingClient_returnsDTO() {
        Profile profile = new Profile("Voronezh, d.123", "8(904)084-47-07");
        Client client = new Client("Ivan", "ivan-st233@mail.ru", profile);

        ProfileDTO profileDTO = new ProfileDTO(
                1L,
                "Voronezh, d.123",
                "8(904)084-47-07",
                1L);

        ClientDTO  clientDTO = new ClientDTO(
                1L,
                "Ivan",
                "ivan-st233@mail.ru",
                LocalDateTime.now(),
                profileDTO,
                Collections.emptyList(),
                Collections.emptyList()
                );

        Mockito.when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        Mockito.when(mapperService.clientToDTO(client)).thenReturn(clientDTO);

        ClientDTO result = clientService.getClientById(1L);

        Assertions.assertEquals("Ivan", result.name());
        Assertions.assertEquals("ivan-st233@mail.ru", result.email());
        Assertions.assertEquals("Voronezh, d.123", result.profile().address());

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verify(mapperService).clientToDTO(client);
    }

    @Test
    void getClientById_notFound_throwsException() {
        Mockito.when(clientRepository.findById(1L)).thenReturn((Optional.empty()));

        Assertions.assertThrows(ResponseStatusException.class, () -> clientService.getClientById(1L));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }
}