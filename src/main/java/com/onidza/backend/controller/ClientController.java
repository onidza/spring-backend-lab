package com.onidza.backend.controller;

import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.service.CacheMode;
import com.onidza.backend.service.client.ClientService;
import com.onidza.backend.service.client.ClientServiceImpl;
import com.onidza.backend.service.client.ManualClientServiceImpl;
import com.onidza.backend.service.client.SpringCachingClientServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientServiceImpl clientServiceImpl;
    private final ManualClientServiceImpl manualClientServiceImpl;
    private final SpringCachingClientServiceImpl springCachingService;

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClient(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Controller called getClient with id: {}", id);

        ClientService service = resolveClientService(cacheMode);
        ClientDTO client = service.getClientById(id);

        return ResponseEntity.ok(client);
    }

    @GetMapping
    public ResponseEntity<ClientsPageDTO> getClientsPage(
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Controller called getClientsPage, page={}, size={}", page, size);

        ClientService service = resolveClientService(cacheMode);
        return ResponseEntity.ok(service.getClientsPage(page, size));
    }

    @PostMapping
    public ResponseEntity<ClientDTO> addClient(
            @Valid @RequestBody ClientDTO clientDTO,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Controller called addClient");

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
        log.info("Controller called updateClient");

        ClientService service = resolveClientService(cacheMode);
        ClientDTO updatedClient = service.updateClient(id, clientDTO);

        return ResponseEntity.ok(updatedClient);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClientById(
            @PathVariable Long id,
            @RequestParam(value = "cacheMode", defaultValue = "NON_CACHE") CacheMode cacheMode
    ) {
        log.info("Controller called deleteClientById with id: {}", id);

        ClientService service = resolveClientService(cacheMode);
        service.deleteClient(id);

        return ResponseEntity.noContent().build();
    }

    private ClientService resolveClientService(CacheMode cacheMode) {
        return switch (cacheMode) {
            case NON_CACHE -> clientServiceImpl;
            case MANUAL -> manualClientServiceImpl;
            case SPRING -> springCachingService;
        };
    }
}
