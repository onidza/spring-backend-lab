package com.onidza.hibernatecore.service;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.model.dto.order.OrderDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Coupon;
import com.onidza.hibernatecore.model.entity.Order;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ClientServiceUnitTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private MapperService mapperService;

    @InjectMocks
    private ClientService clientService;


    @Test
    void getClientById_existingClient_returnsDTO() {
        Client persistentClient = ClientDataFactory.createPersistentClientEntity();
        ClientDTO persistentClientDTO = ClientDataFactory.createPersistentClientDTO();

        Mockito.when(clientRepository.findById(1L)).thenReturn(Optional.of(persistentClient));
        Mockito.when(mapperService.clientToDTO(persistentClient)).thenReturn(persistentClientDTO);

        ClientDTO result = clientService.getClientById(1L);

        Assertions.assertNotNull(result.id());
        Assertions.assertNotNull(result.profile().id());
        Assertions.assertEquals(result.id(), result.profile().clientId());

        Assertions.assertEquals("Ivan", result.name());
        Assertions.assertEquals(persistentClient.getRegistrationDate(), result.registrationDate());
        Assertions.assertEquals("Voronezh, d.123", result.profile().address());

        Assertions.assertNotNull(result.coupons());
        Assertions.assertTrue(result.coupons().isEmpty());

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verify(mapperService).clientToDTO(persistentClient);
    }

    @Test
    void getClientById_notFound_throwsException() {
        Mockito.when(clientRepository.findById(1L)).thenReturn((Optional.empty()));

        Assertions.assertThrows(ResponseStatusException.class,
                () -> clientService.getClientById(1L));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void addClient_successfully_returnDTO() {
        ClientDTO inputClientDTO = ClientDataFactory.createInputClientDTO();
        Client inputClientEntity = ClientDataFactory.createInputClientEntity();
        Client persistentClientEntity = ClientDataFactory.createPersistentClientEntity();
        ClientDTO persistentClientDTO = ClientDataFactory.createPersistentClientDTO();

        Mockito.when(mapperService.clientDTOToEntity(inputClientDTO)).thenReturn(inputClientEntity);
        Mockito.when(clientRepository.save(inputClientEntity)).thenReturn(persistentClientEntity);
        Mockito.when(mapperService.clientToDTO(persistentClientEntity)).thenReturn(persistentClientDTO);

        ClientDTO result = clientService.addClient(inputClientDTO);

        Assertions.assertEquals("Ivan", result.name());
        Assertions.assertEquals(persistentClientDTO.registrationDate(), result.registrationDate());

        Assertions.assertNotNull(result.profile());
        Assertions.assertNotNull(result.coupons());
        Assertions.assertNotNull(result.orders());

        Assertions.assertEquals("Voronezh, d.123", result.profile().address());


        Mockito.verify(mapperService).clientDTOToEntity(inputClientDTO);
        Mockito.verify(mapperService).clientToDTO(persistentClientEntity);
        Mockito.verify(clientRepository).save(inputClientEntity);
    }

    @Test
    void addClient_unique_throwsException() {
        ClientDTO inputClientDTO = ClientDataFactory.createInputClientDTO();
        Client inputClientEntity = ClientDataFactory.createInputClientEntity();

        Mockito.when(mapperService.clientDTOToEntity(inputClientDTO)).thenReturn(inputClientEntity);
        Mockito.when(clientRepository.save(inputClientEntity))
                .thenThrow(new DataIntegrityViolationException("Unique index violation"));

        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> clientService.addClient(inputClientDTO));

        Mockito.verify(clientRepository).save(inputClientEntity);
    }

    @Test
    void updateClient_successfully_returnDTO() {
        Client persistentClientEntity = ClientDataFactory.createPersistentClientEntity();
        ClientDTO distinctInputClientEntity = ClientDataFactory.createDistinctInputClientDTO();

        Mockito.when(clientRepository.findById(1L)).thenReturn(Optional.of(persistentClientEntity));

        Mockito.when(mapperService.couponDTOToEntity(Mockito.any()))
                .thenAnswer(i -> {
                    CouponDTO dto = i.getArgument(0);
                    return new Coupon(dto.code(), dto.discount(), dto.expirationDate());
                });

        Mockito.when(mapperService.orderDTOToEntity(Mockito.any()))
                .thenAnswer(i -> {
                    OrderDTO dto = i.getArgument(0);
                    return new Order(dto.orderDate(), dto.totalAmount(), dto.status());
                });

        Mockito.when(mapperService.clientToDTO(persistentClientEntity))
                .thenReturn(distinctInputClientEntity);

        Mockito.when(clientRepository.save(persistentClientEntity)).thenReturn(persistentClientEntity);

        ClientDTO result = clientService.updateClient(1L, distinctInputClientEntity);

        Assertions.assertEquals("Sasha", result.name());
        Assertions.assertEquals("sasha@mail.ru", result.email());
        Assertions.assertEquals("8(111)111-111-11", result.profile().phone());

        Assertions.assertEquals(new BigDecimal("16445"), result.orders().get(0).totalAmount());
        Assertions.assertEquals("NEW CODE777", result.coupons().get(0).code());
        Assertions.assertEquals(result.id(), result.orders().get(0).clientId());

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verify(mapperService).couponDTOToEntity(Mockito.any());
        Mockito.verify(mapperService).orderDTOToEntity(Mockito.any());
    }

    @Test
    void updateClient_notFound_throwsException() {
        ClientDTO inputClientDTO = ClientDataFactory.createInputClientDTO();

        Mockito.when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> clientService.updateClient(1L, inputClientDTO));

        Mockito.verify(clientRepository).findById(1L);
    }

    @Test
    void deleteClient_successfully_doNothing() {
        Mockito.doNothing().when(clientRepository).deleteById(1L);
        clientService.deleteClient(1L);
        Mockito.verify(clientRepository).deleteById(1L);
    }

    @Test
    void deleteClient_notFound_doNothing() {
        Mockito.doNothing().when(clientRepository).deleteById(999L);
        clientService.deleteClient(999L);
        Mockito.verify(clientRepository).deleteById(999L);
    }
}