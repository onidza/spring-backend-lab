package com.onidza.backend.service.client;

import com.onidza.backend.config.CacheKeys;
import com.onidza.backend.config.CacheVersionKeys;
import com.onidza.backend.config.CacheVersionService;
import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.client.ClientsPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Profile;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.service.TransactionAfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RequiredArgsConstructor
@Service
public class SpringCachingClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final MapperService mapperService;

    private final TransactionAfterCommitExecutor afterCommitExecutor;
    private final CacheVersionService versionService;

    @Override
    @Cacheable(
            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
            key = "'id:' + #id",
            condition = "#id > 0"
    )
    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long id) {
        log.info("Service called getClientById with id: {}", id);

        return mapperService
                .clientToDTO(clientRepository.findById(id)
                        .orElseThrow(()
                                -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Client not found")));
    }

    @Override
    @Cacheable(
            cacheNames = CacheKeys.CLIENTS_PAGE_VER_KEY,
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

        if (client.getProfile() != null) {
            client.getProfile().setClient(client);
        }

        Client saved = clientRepository.save(client);


        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
            log.info("Key {} was incremented.", CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
        });


        return mapperService.clientToDTO(saved);
    }

    @Override
    @CachePut(
            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
            key = "'id:' + #result.id()",
            condition = "#id > 0"
    )
    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        log.info("Service called updateClient with id: {}", id);

        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Client not found"));

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

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
            log.info("Key {} was incremented.", CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
        });

        return mapperService.clientToDTO(clientRepository.save(existing));
    }

    @CacheEvict(
            cacheNames = CacheKeys.CLIENT_KEY_PREFIX,
            key = "'id:' +  #id",
            condition = "#id > 0"
    )
    @Override
    @Transactional
    public void deleteClient(Long id) {
        log.info("Service called deleteClient with id: {}", id);
        clientRepository.deleteById(id);

        afterCommitExecutor.run(() -> {
            versionService.bumpVersion(CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
            log.info("Key {} was incremented.", CacheVersionKeys.CLIENTS_PAGE_VER_KEY);
        });
    }
}
