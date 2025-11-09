package com.onidza.hibernatecore.service;


import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Profile;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import com.onidza.hibernatecore.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ProfileRepository profileRepository;
    private final MapperService mapperService;

    public ClientDTO getClientById(Long id) {
        return mapperService
                .clientToDTO(clientRepository.findById(id)
                        .orElseThrow(()
                                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found")));
    }

    public List<ClientDTO> getAllClients() {
        return clientRepository
                .findAll()
                .stream()
                .map(mapperService::clientToDTO)
                .collect(Collectors.toList());
    }

    public ClientDTO addClient(ClientDTO clientDTO) {
        Client client = clientRepository
                .save(mapperService.clientDTOToEntity(clientDTO));

        if (client.getProfile() != null) {
            client.getProfile().setClient(client);
            profileRepository.save(client.getProfile());
        }
        return mapperService.clientToDTO(client);
    }

    public ClientDTO updateClient(Long id, ClientDTO updated) {
        Client existing = clientRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        existing.setName(updated.name());
        existing.setEmail(updated.email());

        Profile existingProfile = existing.getProfile();
        if (existing.getProfile() != null && updated.profile() != null) {
            existingProfile.setAddress(updated.profile().address());
            existingProfile.setPhone(updated.profile().phone());
        }

        if (updated.coupons() != null) {
            existing.getCoupons().clear();
            existing.setCoupons(updated.coupons()
                    .stream()
                    .map(mapperService::couponDTOToEntity)
                    .peek(coupon -> coupon.getClients().add(existing))
                    .collect(Collectors.toList())
            );
        }

        Client saved = clientRepository.save(existing);
        return mapperService.clientToDTO(saved);
    }

    public void deleteClient(@PathVariable Long id) {
        clientRepository.deleteById(id);
    }
}
