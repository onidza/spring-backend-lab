package com.onidza.backend.service.client;

import com.onidza.backend.cache.config.spring.CacheSpringKeys;
import com.onidza.backend.model.dto.client.ClientActionPart;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.dto.client.events.ClientAddEvent;
import com.onidza.backend.model.dto.client.events.ClientDeletedEvent;
import com.onidza.backend.model.dto.client.events.ClientUpdateEvent;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpringCachingClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final MapperService mapperService;
    private final ApplicationEventPublisher publisher;

    private static final String CLIENT_NOT_FOUND = "Client not found";

    @Override
    @Cacheable(
            cacheNames = CacheSpringKeys.CLIENT_KEY_PREFIX,
            key = "#id",
            condition = "#id > 0"
    )
    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long id) {
        log.info("Service called getClientById with id: {}", id);

        return mapperService
                .clientToDTO(clientRepository.findById(id)
                        .orElseThrow(()
                                -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                CLIENT_NOT_FOUND)));
    }

    @Override
    @Cacheable(
            cacheNames = CacheSpringKeys.CLIENTS_PAGE_PREFIX,
            keyGenerator = "clientPageKeyGen"
    )
    @Transactional(readOnly = true)
    public ClientsPageDTO getClientsPage(int page, int size) {
        log.info("Service called getClientsPage");

        int safeSize = Math.min(Math.max(size, 1), 20);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
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
        log.info("Service called addClient with name: {}", clientDTO.name());

        Client client = mapperService.clientDTOToEntity(clientDTO);
        client.getProfile().setClient(client);
        Client saved = clientRepository.save(client);

        EnumSet<ClientActionPart> parts = EnumSet.noneOf(ClientActionPart.class);

        if (clientDTO.coupons() != null && !clientDTO.coupons().isEmpty())
            parts.add(ClientActionPart.COUPONS);

        if (clientDTO.orders() != null && !clientDTO.orders().isEmpty())
            parts.add(ClientActionPart.ORDERS);

        publisher.publishEvent(new ClientAddEvent(parts));

        return mapperService.clientToDTO(saved);
    }

    @Override
    @CachePut(
            cacheNames = CacheSpringKeys.CLIENT_KEY_PREFIX,
            key = "#result.id()",
            condition = "#id > 0"
    )
    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        log.info("Service called updateClient with id: {}", id);

        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        CLIENT_NOT_FOUND));

        existing.setName(clientDTO.name());
        existing.setEmail(clientDTO.email());

        Set<Long> orderIdsToEvict = new HashSet<>();
        Set<Long> couponIdsToEvict = new HashSet<>();
        Long profileId = existing.getProfile().getId();

        EnumSet<ClientActionPart> parts = EnumSet.noneOf(ClientActionPart.class);

        boolean couponsTouched = clientDTO.coupons() != null && !clientDTO.coupons().isEmpty();
        boolean orderTouched = clientDTO.orders() != null && !clientDTO.orders().isEmpty();

        Profile existingProfile = existing.getProfile();
        if (existing.getProfile() != null && clientDTO.profile() != null) {
            parts.add(ClientActionPart.PROFILE);

            existingProfile.setAddress(clientDTO.profile().address());
            existingProfile.setPhone(clientDTO.profile().phone());
        }

        if (couponsTouched) {
            parts.add(ClientActionPart.COUPONS);
            couponIdsToEvict = getCouponsIdsForEvict(existing.getCoupons(), clientDTO.coupons());

            existing.getCoupons().clear();
            clientDTO.coupons()
                    .stream()
                    .map(mapperService::couponDTOToEntity)
                    .forEach(coupon -> {
                        existing.getCoupons().add(coupon);
                        coupon.getClients().add(existing);
                    });
        }

        if (orderTouched) {
            parts.add(ClientActionPart.ORDERS);
            orderIdsToEvict = getOrdersIdsForEvict(existing.getOrders(), clientDTO.orders());

            existing.getOrders().clear();
            clientDTO.orders()
                    .stream()
                    .map(mapperService::orderDTOToEntity)
                    .forEach(order -> {
                        existing.getOrders().add(order);
                        order.setClient(existing);
                    });
        }

        publisher.publishEvent(new ClientUpdateEvent(
                profileId,
                parts,
                orderIdsToEvict,
                couponIdsToEvict)
        );

        return mapperService.clientToDTO(clientRepository.save(existing));
    }

    @CacheEvict(
            cacheNames = CacheSpringKeys.CLIENT_KEY_PREFIX,
            key = "#id",
            condition = "#id > 0"
    )
    @Override
    @Transactional
    public void deleteClient(Long id) {
        log.info("Service called deleteClient with id: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        CLIENT_NOT_FOUND));

        Set<Long> orderIdsForEvict = getOrdersIdsForEvict(client.getOrders(), Collections.emptyList());
        EnumSet<ClientActionPart> parts = EnumSet.noneOf(ClientActionPart.class);

        if (client.getCoupons() != null && !client.getCoupons().isEmpty()) parts.add(ClientActionPart.COUPONS);
        if (client.getOrders() != null && !client.getOrders().isEmpty()) parts.add(ClientActionPart.ORDERS);

        publisher.publishEvent(new ClientDeletedEvent(
                client.getProfile().getId(),
                parts,
                orderIdsForEvict
        ));

        clientRepository.deleteById(id);
    }

    private Set<Long> getCouponsIdsForEvict(Set<Coupon> oldSet, List<CouponDTO> newSet) {
        Set<Long> res = new HashSet<>();

        Set<Long> oldCouponsIds = oldSet.stream()
                .map(Coupon::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> newCouponsIds = newSet.stream()
                .map(CouponDTO::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        res.addAll(oldCouponsIds);
        res.addAll(newCouponsIds);

        return res;
    }

    private Set<Long> getOrdersIdsForEvict(Set<Order> oldSet, List<OrderDTO> newSet) {
        Set<Long> res = new HashSet<>();

        Set<Long> oldOrdersIds = oldSet.stream()
                .map(Order::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> newOrderIds = newSet.stream()
                .map(OrderDTO::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        res.addAll(oldOrdersIds);
        res.addAll(newOrderIds);

        return res;
    }
}
