package com.onidza.backend.service.client;

import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.dto.client.ClientsUpdateDTO;
import com.onidza.backend.model.events.ActionPart;
import com.onidza.backend.model.events.client.ClientAddEvent;
import com.onidza.backend.model.events.client.ClientDeletedEvent;
import com.onidza.backend.model.events.client.ClientUpdateEvent;
import com.onidza.backend.model.dto.profile.ProfileDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Coupon;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final MapperService mapperService;
    private final ApplicationEventPublisher publisher;

    private static final String CLIENT_NOT_FOUND = "Client not found";

    @Override
    @Cacheable(
            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
            key = "#clientId"
    )
    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long clientId) {
        log.info("ClientServiceImpl called getClientById with id = {}", clientId);

        return mapperService
                .clientToDTO(clientRepository.findById(clientId)
                        .orElseThrow(()
                                -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                CLIENT_NOT_FOUND)));
    }

    @Override
    @Cacheable(
            cacheNames = CacheKeys.CLIENTS_PAGE_PREFIX,
            keyGenerator = "clientPageKeyGen"
    )
    @Transactional(readOnly = true)
    public ClientsPageDTO getClientsPage(int page, int size) {
        log.info("ClientServiceImpl called getClientsPage, page = {}, size = {}", page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "id"));

        Page<ClientDTO> result = clientRepository.findAll(pageable)
                .map(mapperService::clientToDTO);

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
        log.info("ClientServiceImpl called addClient with name = {}", clientDTO.name());

        Client saved = clientRepository.save(mapperService.clientDTOToEntity(clientDTO));

        ClientAddEvent event = buildAddEvent(clientDTO);
        publisher.publishEvent(event);

        return mapperService.clientToDTO(saved);
    }

    @Override
    @CachePut(
            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
            key = "#result.id()"
    )
    @Transactional
    public ClientDTO updateClientById(Long clientId, ClientsUpdateDTO clientDTO) {
        log.info("ClientServiceImpl called updateClientById with id = {}", clientId);

        Client existing = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        CLIENT_NOT_FOUND));

        applyClientUpdate(clientDTO, existing);

        ClientUpdateEvent event = buildClientUpdateEvent(existing);
        publisher.publishEvent(event);

        return mapperService.clientToDTO(clientRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteClientById(Long clientId) {
        log.info("ClientServiceImpl called deleteClientById with id = {}", clientId);

        Client existing = clientRepository.findById(clientId)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        CLIENT_NOT_FOUND));

        ClientDeletedEvent invalidateDeleteEvent = buildClientDeletedEvent(
                clientId,
                existing
        );
        publisher.publishEvent(invalidateDeleteEvent);

        detachClientRelationsBeforeDelete(existing);

        clientRepository.deleteById(clientId);
    }

    private ClientAddEvent buildAddEvent(ClientDTO clientDTO) {
        EnumSet<ActionPart> parts = EnumSet.noneOf(ActionPart.class);

        if (!CollectionUtils.isEmpty(clientDTO.orders()))
            parts.add(ActionPart.ORDERS);

        if (!CollectionUtils.isEmpty(clientDTO.coupons()))
            parts.add(ActionPart.COUPONS);

        return new ClientAddEvent(parts);
    }

    private ClientUpdateEvent buildClientUpdateEvent(Client existing) {
        return new ClientUpdateEvent(
                existing.getId(),
                existing.getProfile().getId()
        );
    }

    private void detachClientRelationsBeforeDelete(Client existing) {
        if (existing.getCoupons().isEmpty()) {
            return;
        }

        new HashSet<>(existing.getCoupons())
                .forEach(existing::removeBiCouponClient);

        existing.getCoupons().clear();
    }

    private void applyClientUpdate(
            ClientsUpdateDTO clientDTO,
            Client existing
    ) {
        existing.updateInfo(clientDTO.name(), clientDTO.email());

        ProfileDTO profileDTO = clientDTO.profile();

        existing.getProfile().updateInfo(
                profileDTO.address(),
                profileDTO.phone()
        );
    }

    private ClientDeletedEvent buildClientDeletedEvent(
            Long clientId,
            Client existing
    ) {
        EnumSet<ActionPart> parts = EnumSet.noneOf(ActionPart.class);
        Long profileId = existing.getProfile().getId();

        Set<Long> orderIds = existing.getOrders()
                .stream()
                .map(Order::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!orderIds.isEmpty()) {
            parts.add(ActionPart.ORDERS);
        }

        Set<Long> couponIds = existing.getCoupons()
                .stream()
                .map(Coupon::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!couponIds.isEmpty()) {
            parts.add(ActionPart.COUPONS);
        }

        return new ClientDeletedEvent(
                clientId,
                profileId,
                orderIds,
                couponIds,
                parts
        );
    }
}
