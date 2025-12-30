package com.onidza.hibernatecore.service.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Profile;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
@Service
public class ManualClientService implements ClientService {

    private final MapperService mapperService;
    private final ClientRepository clientRepository;
    private final EntityManager entityManager;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "client:";
    private static final long CACHE_TTL_MINUTES = 1;

    private static final String ALL_CLIENTS_KEY = "clients:all:v1";
    private static final Duration ALL_CLIENTS_TTL = Duration.ofMinutes(1);

    //this one method for example with stringRedisTemplate
    @Override
    public ClientDTO getClientById(Long id) {
        log.info("Called getClientById with id: {}", id);

        var cacheKey = CACHE_KEY_PREFIX + id;
        String objFromCache = stringRedisTemplate.opsForValue().get(cacheKey);

        try {
            if (objFromCache != null) {
                log.info("Returned client from cash with id: {}", id);
                return objectMapper.readValue(objFromCache, ClientDTO.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to read client from cache for key {}", cacheKey, e);
        }

        log.info("Returned client from db with id: {}", id);
        Client clientFromDb = clientRepository.findById(id)
                .orElseThrow(()
                        -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(mapperService.clientToDTO(clientFromDb)),
                    CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
            log.info("getClientById was cached...");

        } catch (JsonProcessingException e) {
            log.warn("Failed to write client to cache for key {}", cacheKey, e);
        }

        return mapperService.clientToDTO(clientFromDb);
    }

    @Override
    public List<ClientDTO> getAllClients() {
        log.info("Called getAllClients");

        Object objFromCache = redisTemplate.opsForValue().get(ALL_CLIENTS_KEY);
        if (objFromCache instanceof List<?> raw) {

            List<ClientDTO> dtoList = raw.stream()
                    .map(o -> objectMapper.convertValue(o, ClientDTO.class)) //* for fix a deserialization List<LinkedHashMap<String, Object>>
                    .toList();

            log.info("Returned dtoList from cache with size={}", dtoList.size());
            return dtoList;
        }

        List<Client> clients = entityManager.createQuery(
                """
                        SELECT DISTINCT c FROM Client c
                        LEFT JOIN FETCH c.profile
                        LEFT JOIN FETCH c.orders
                        LEFT JOIN FETCH c.coupons
                        """, Client.class
        ).getResultList();

        List<ClientDTO> dtoList = clients.stream()
                .map(mapperService::clientToDTO)
                .toList();

        redisTemplate.opsForValue().set(ALL_CLIENTS_KEY, dtoList, ALL_CLIENTS_TTL);
        log.info("getAllClients was cached...");

        log.info("Returned dtoList from bd with size={}", clients.size());
        return dtoList;
    }

    @Override
    @Transactional
    public ClientDTO addClient(ClientDTO clientDTO) {
        log.info("Called addClient with name: {}", clientDTO.name());

        Client client = mapperService.clientDTOToEntity(clientDTO);

        if (client.getProfile() != null) {
            client.getProfile().setClient(client);
        }

        Client saved = clientRepository.save(client);
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

        ClientDTO updated = mapperService.clientToDTO(clientRepository.save(existing));

        redisTemplate.delete(CACHE_KEY_PREFIX + existing.getId());
        redisTemplate.delete(ALL_CLIENTS_KEY);

        log.info("Updated client was invalidated in cache with id={}", existing.getId());
        log.info("Updated client in getAllList was invalidated too with key={}", ALL_CLIENTS_KEY);

        return updated;
    }

    @Override
    public void deleteClient(Long id) {
        log.info("Called deleteClient with id: {}", id);

        if (!clientRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }

        clientRepository.deleteById(id);

        redisTemplate.delete(CACHE_KEY_PREFIX + id);
        redisTemplate.delete(ALL_CLIENTS_KEY);

        log.info("Deleted client was invalidated in cache with id={}", id);
        log.info("Deleted client in getAllList was invalidated too with key={}", ALL_CLIENTS_KEY);
    }
}
