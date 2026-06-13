package com.onidza.backend.controller;

import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.dto.client.ClientsUpdateDTO;
import com.onidza.backend.service.client.ClientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Validated
public class ClientController {

    private final ClientService clientService;

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClient(
            @PathVariable @Positive Long id
    ) {
        log.info("ClientController called getClient with id = {}", id);
        ClientDTO client = clientService.getClient(id);

        return ResponseEntity.ok(client);
    }

    @GetMapping
    public ResponseEntity<ClientsPageDTO> getClientsPage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("ClientController called getClientsPage, page = {}, size = {}", page, size);

        return ResponseEntity.ok(clientService.getClientsPage(page, size));
    }

    @PostMapping
    public ResponseEntity<ClientDTO> createClient(
            @Valid @RequestBody ClientDTO clientDTO
    ) {
        log.info("ClientController called createClient");
        ClientDTO savedClient = clientService.createClient(clientDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedClient);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> updateClient(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ClientsUpdateDTO clientDTO
    ) {
        log.info("ClientController called updateClient with id = {}", id);
        ClientDTO updatedClient = clientService.updateClient(id, clientDTO);

        return ResponseEntity.ok(updatedClient);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(
            @PathVariable @Positive Long id
    ) {
        log.info("ClientController called deleteClient with id = {}", id);
        clientService.deleteClient(id);

        return ResponseEntity.noContent().build();
    }
}
