package com.onidza.hibernatecore.controller;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.service.CacheMode;
import com.onidza.hibernatecore.service.client.ClientService;
import com.onidza.hibernatecore.service.client.ClientServiceImpl;
import com.onidza.hibernatecore.service.client.ManualClientServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientServiceImpl clientServiceImpl;
    private final ManualClientServiceImpl manualClientServiceImpl;

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClient(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getClient with id: {}", id);

        ClientService service = resolveClientService(cacheMode);
        ClientDTO client = service.getClientById(id);

        return ResponseEntity.ok(client);
    }

    @GetMapping
    public ResponseEntity<List<ClientDTO>> getAllClients(
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called getAllClients");

        ClientService service = resolveClientService(cacheMode);
        List<ClientDTO> clients = service.getAllClients();

        return ResponseEntity.ok(clients);
    }

    @PostMapping
    public ResponseEntity<ClientDTO> addClient(
            @Valid @RequestBody ClientDTO clientDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called addClient with name: {}", clientDTO.name());

        ClientService service = resolveClientService(cacheMode);
        ClientDTO savedClient = service.addClient(clientDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedClient);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> updateClient(
            @PathVariable Long id,
            @RequestBody ClientDTO clientDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called updateClient with name: {}", clientDTO.name());

        ClientService service = resolveClientService(cacheMode);
        ClientDTO updatedClient = service.updateClient(id, clientDTO);

        return ResponseEntity.ok(updatedClient);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClientById(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Called deleteClientById with id: {}", id);

        ClientService service = resolveClientService(cacheMode);
        service.deleteClient(id);

        return ResponseEntity.noContent().build();
    }

    private ClientService resolveClientService(CacheMode cacheMode) {
        return switch (cacheMode) {
            case NON_CACHE -> clientServiceImpl;
            case MANUAL -> manualClientServiceImpl;
            case SPRING -> throw new UnsupportedOperationException("Have no such a service");
        };
    }
}
