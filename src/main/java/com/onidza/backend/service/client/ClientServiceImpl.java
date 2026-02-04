package com.onidza.backend.service.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final ObjectMapper objectMapper; //todo чек и убрать

    @Override
    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long id) {
        log.info("Called getClientById with id: {}", id);

        return mapperService
                .clientToDTO(clientRepository.findById(id)
                        .orElseThrow(()
                                -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientsPageDTO getClientsPage(int page, int size) {
        log.info("Called getClientsPage");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "id"));

        Page<ClientDTO> result = clientRepository.findAll(pageable).map(mapperService::clientToDTO);

        return new ClientsPageDTO(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Override
    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        log.info("Called addClient with name: {}", clientDTO.name());

        Client client = mapperService.clientDTOToEntity(clientDTO);

        client.getProfile().setClient(client);

        Client saved = clientRepository.save(client);

        log.info("ObjectMapper = {}", objectMapper); //todo чек и убрать
        log.info("WRITE_DATES_AS_TIMESTAMPS = {}",
                objectMapper.getSerializationConfig().isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

        return mapperService.clientToDTO(saved);
    }

    @Override
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

    @Override
    public void deleteClient(Long id) {
        log.info("Called deleteClient with id: {}", id);
        clientRepository.deleteById(id);
    }
}
