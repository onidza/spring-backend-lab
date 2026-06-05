package com.onidza.backend.service.client;

import com.onidza.backend.config.cache.keys.CacheKeys;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.dto.client.events.ActionPart;
import com.onidza.backend.model.dto.client.events.client.ClientAddEvent;
import com.onidza.backend.model.dto.client.events.client.ClientDeletedEvent;
import com.onidza.backend.model.dto.client.events.client.ClientUpdateEvent;
import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.order.OrderDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Coupon;
import com.onidza.backend.model.entity.Order;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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

import java.util.*;
import java.util.function.Function;
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

        EnumSet<ActionPart> parts = EnumSet.noneOf(ActionPart.class);

        if (!CollectionUtils.isEmpty(clientDTO.coupons()))
            parts.add(ActionPart.COUPONS);

        if (!CollectionUtils.isEmpty(clientDTO.orders()))
            parts.add(ActionPart.ORDERS);

        publisher.publishEvent(new ClientAddEvent(parts));

        return mapperService.clientToDTO(saved);
    }

    @Override
    @CachePut(
            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
            key = "#result.id()"
    )
    @Transactional
    public ClientDTO updateClientById(Long clientId, ClientDTO clientDTO) {
        log.info("ClientServiceImpl called updateClientById with id = {}", clientId);

        Client existing = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        CLIENT_NOT_FOUND));

        existing.setName(clientDTO.name());
        existing.setEmail(clientDTO.email());

        ClientUpdateEvent invalidateUpdateEvent = applyClientUpdateAndBuildEvent(clientId, clientDTO, existing);

        publisher.publishEvent(invalidateUpdateEvent);

        return mapperService.clientToDTO(clientRepository.save(existing));
    }

    @CacheEvict(
            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
            key = "#id"
    )
    @Override
    @Transactional
    public void deleteClientById(Long id) {
        log.info("ClientServiceImpl called deleteClientById with id = {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        CLIENT_NOT_FOUND));

        EnumSet<ActionPart> parts = EnumSet.noneOf(ActionPart.class);

        if (!CollectionUtils.isEmpty(client.getCoupons())) parts.add(ActionPart.COUPONS);
        if (!CollectionUtils.isEmpty(client.getOrders())) parts.add(ActionPart.ORDERS);

        publisher.publishEvent(new ClientDeletedEvent(
                client.getProfile().getId(),
                parts,
                getOrdersIdsForEvict(client.getOrders(), Collections.emptyList())
        ));

        clientRepository.deleteById(id);
    }

    private ClientUpdateEvent applyClientUpdateAndBuildEvent(
            Long clientId,
            ClientDTO clientDTO,
            Client existing
    ) {
        EnumSet<ActionPart> parts = EnumSet.noneOf(ActionPart.class);
        Long profileIdForInvalidate = processProfileDiffs(clientDTO, existing, parts);
        Set<Long> orderIdsForInvalidate = processOrderDiffs(clientDTO, parts, existing);
        Set<Long> couponIdsForInvalidate = processCouponDiffs(clientDTO, parts, existing);

        return new ClientUpdateEvent(
                clientId,
                profileIdForInvalidate,
                orderIdsForInvalidate,
                couponIdsForInvalidate,
                parts
        );
    }

    private Long processProfileDiffs(
            ClientDTO clientDTO,
            Client existing,
            EnumSet<ActionPart> parts
    ) {
        Profile existingProfile = existing.getProfile();

        parts.add(ActionPart.PROFILE);

        existingProfile.updateInfo(
                clientDTO.profile().address(),
                clientDTO.profile().phone()
        );

        return existingProfile.getId();
    }

    private Set<Long> processOrderDiffs(
            ClientDTO clientDTO,
            EnumSet<ActionPart> parts,
            Client existing
    ) {
        if (CollectionUtils.isEmpty(clientDTO.orders())) {
            return Set.of();
        }

        parts.add(ActionPart.ORDERS);

        Set<Long> orderIdsToEvict = collectIdsForEvict(
                existing.getOrders(),
                clientDTO.orders(),
                Order::getId,
                OrderDTO::id
        );

        existing.getOrders().clear();

        clientDTO.orders()
                .stream()
                .map(mapperService::orderDTOToEntity)
                .forEach(existing::setBidirectionalOrderClient);

        return orderIdsToEvict;
    }

    private Set<Long> processCouponDiffs(
            ClientDTO clientDTO,
            EnumSet<ActionPart> parts,
            Client existing
    ) {
        if (CollectionUtils.isEmpty(clientDTO.coupons()))
            return Set.of();


        parts.add(ActionPart.COUPONS);

        Set<Long> couponIdsToEvict = collectIdsForEvict(
                existing.getCoupons(),
                clientDTO.coupons(),
                Coupon::getId,
                CouponDTO::id
        );

        existing.getCoupons().clear();

        clientDTO.coupons()
                .stream()
                .map(mapperService::couponDTOToEntity)
                .forEach(existing::setBidirectionalCouponClient);

        return couponIdsToEvict;
    }

    private <E, D> Set<Long> collectIdsForEvict(
            Collection<E> oldItems,
            Collection<D> newItems,
            Function<E, Long> oldIdExtractor,
            Function<D, Long> newIdExtractor
    ) {
        Set<Long> ids = new HashSet<>();

        oldItems.stream()
                .map(oldIdExtractor)
                .filter(Objects::nonNull)
                .forEach(ids::add);

        newItems.stream()
                .map(newIdExtractor)
                .filter(Objects::nonNull)
                .forEach(ids::add);

        return ids;
    }
}
