package com.onidza.hibernatecore.service.Coupon;

import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Coupon;
import com.onidza.hibernatecore.model.mapper.MapperService;
import com.onidza.hibernatecore.repository.ClientRepository;
import com.onidza.hibernatecore.repository.CouponRepository;
import com.onidza.hibernatecore.service.coupon.CouponServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CouponServiceUnitTest {

    @Mock
    private MapperService mapperService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private CouponServiceImpl couponServiceImpl;

    @Test
    void getCouponById_returnCouponDTOWithRelations() {
        Coupon persistentCoupon = CouponDataFactory.createPersistentCouponEntity();
        CouponDTO persistentCouponDTO = CouponDataFactory.createPersistentCouponDTO();

        Mockito.when(couponRepository.findById(persistentCoupon.getId()))
                .thenReturn(Optional.of(persistentCoupon));

        Mockito.when(mapperService.couponToDTO(persistentCoupon))
                .thenReturn(persistentCouponDTO);

        CouponDTO result = couponServiceImpl.getCouponById(persistentCoupon.getId());

        Assertions.assertNotNull(result.id());
        Assertions.assertEquals(result.code(), persistentCoupon.getCode());
        Assertions.assertEquals(result.expirationDate(), persistentCoupon.getExpirationDate());

        Mockito.verify(couponRepository).findById(1L);
        Mockito.verify(mapperService).couponToDTO(persistentCoupon);
    }

    @Test
    void getCouponById_notFound() {
        Mockito.when(couponRepository.findById(Mockito.anyLong()))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> couponServiceImpl.getCouponById(1L));

        Mockito.verify(couponRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void getAllCouponsByClientId_returnListOfCouponsDTOWithRelations() {
        Coupon persistentCouponEntity = CouponDataFactory.createPersistentCouponEntity();
        Coupon persistentDistinctCouponEntity = CouponDataFactory.createDistinctPersistentCouponEntity();

        CouponDTO persistentOrderDTO = CouponDataFactory.createPersistentCouponDTO();
        CouponDTO persistentDistinctOrderDTO = CouponDataFactory.createDistinctPersistentClientDTO();

        Mockito.when(couponRepository.findAll())
                .thenReturn(List.of(persistentCouponEntity, persistentDistinctCouponEntity));
        Mockito.when(mapperService.couponToDTO(persistentCouponEntity))
                .thenReturn(persistentOrderDTO);
        Mockito.when(mapperService.couponToDTO(persistentDistinctCouponEntity))
                .thenReturn(persistentDistinctOrderDTO);

        List<CouponDTO> result = couponServiceImpl.getAllCoupons();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(orderDTO
                -> orderDTO.id().equals(1L)));

        Assertions.assertTrue(result.stream().anyMatch(orderDTO
                -> orderDTO.id().equals(2L)));

        Assertions.assertTrue(result.stream().anyMatch(c -> c.discount() == 8.8f));

        Mockito.verify(couponRepository).findAll();
        Mockito.verify(mapperService, Mockito.times(2))
                .couponToDTO(Mockito.any(Coupon.class));
    }

    @Test
    void getAllCoupons_emptyList() {
        Mockito.when(couponRepository.findAll()).thenReturn(Collections.emptyList());

        List<CouponDTO> result = couponServiceImpl.getAllCoupons();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

        Mockito.verify(couponRepository).findAll();
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void getAllCouponsByClientId_returnListCouponsDTOWithRelations() {
        Client persistentClientWithOrders = CouponDataFactory.createPersistClientWithTwoCoupons();

        Mockito.when(clientRepository.findById(persistentClientWithOrders.getId()))
                .thenReturn(Optional.of(persistentClientWithOrders));

        Mockito.when(mapperService.couponToDTO(Mockito.any(Coupon.class)))
                .thenAnswer(invocation -> {
                    Coupon coupon = invocation.getArgument(0);
                    return new CouponDTO(
                            coupon.getId(),
                            coupon.getCode(),
                            coupon.getDiscount(),
                            coupon.getExpirationDate(),
                            List.of(persistentClientWithOrders.getId())
                    );
                });

        List<CouponDTO> result = couponServiceImpl.getAllCouponsByClientId(persistentClientWithOrders.getId());

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().allMatch(coupon ->
                coupon.clientsId().get(0).equals(persistentClientWithOrders.getId())
        ));

        Assertions.assertTrue(result.stream().anyMatch(coupon ->
                coupon.discount() == 8.8f || coupon.discount() == 2.1f)
        );

        Mockito.verify(clientRepository).findById(persistentClientWithOrders.getId());
        Mockito.verify(mapperService, Mockito.times(2))
                .couponToDTO(Mockito.any(Coupon.class));
    }

    @Test
    void getAllCouponsByClientId_notFound_throwsException() {
        Mockito.when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> couponServiceImpl.getAllCouponsByClientId(1L));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void addOrderToClient_returnOrderDTOWithRelations() {
        Client client = CouponDataFactory.createPersistClientWithTwoCoupons();
        CouponDTO couponDTOForAdd = CouponDataFactory.createCouponDTOForAdd();
        Coupon couponEntityForAdd = CouponDataFactory.createPersistentCouponEntityForAdd();
        CouponDTO couponDTOAfterAdd = CouponDataFactory.createCouponDTOAfterAdd();

        Mockito.when(mapperService.couponDTOToEntity(couponDTOForAdd))
                .thenReturn(couponEntityForAdd);

        Mockito.when(clientRepository.findById(client.getId()))
                .thenReturn(Optional.of(client));

        Mockito.when(couponRepository.save(couponEntityForAdd))
                .thenReturn(couponEntityForAdd);

        Mockito.when(mapperService.couponToDTO(couponEntityForAdd))
                .thenReturn(couponDTOAfterAdd);

        CouponDTO result = couponServiceImpl.addCouponToClientById(client.getId(), couponDTOForAdd);

        Assertions.assertEquals(couponDTOForAdd.discount(), result.discount());
        Assertions.assertEquals(3, client.getCoupons().size());

        Mockito.verify(mapperService).couponDTOToEntity(couponDTOForAdd);
        Mockito.verify(clientRepository).findById(client.getId());
        Mockito.verify(couponRepository).save(couponEntityForAdd);
        Mockito.verify(mapperService).couponToDTO(couponEntityForAdd);
    }

    @Test
    void addCouponToClient_ClientNotFound_throwsExceptions() {
        CouponDTO couponDTOForAdd = CouponDataFactory.createCouponDTOForAdd();

        Mockito.when(clientRepository.findById(1L))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> couponServiceImpl.addCouponToClientById(1L, couponDTOForAdd));

        Mockito.verify(clientRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
        Mockito.verifyNoInteractions(couponRepository);
    }

    @Test
    void updateCouponByCouponId_returnCouponDTOWithRelations() {
        Coupon persistCoupon = CouponDataFactory.createPersistentCouponEntity();
        CouponDTO forUpdate = CouponDataFactory.createCouponDTOForUpdate();
        CouponDTO couponAfterUpdate = CouponDataFactory.createCouponDTOAfterUpdate();

        Mockito.when(couponRepository.findById(persistCoupon.getId()))
                .thenReturn(Optional.of(persistCoupon));

        Mockito.when(mapperService.couponToDTO(persistCoupon))
                .thenReturn(couponAfterUpdate);

        CouponDTO result = couponServiceImpl.updateCouponByCouponId(persistCoupon.getId(), forUpdate);

        Assertions.assertEquals(forUpdate.code(), result.code());
        Assertions.assertEquals(persistCoupon.getId(), result.id());
        Assertions.assertEquals(forUpdate.expirationDate(), result.expirationDate());

        Mockito.verify(couponRepository).findById(persistCoupon.getId());
        Mockito.verify(mapperService).couponToDTO(persistCoupon);
    }

    @Test
    void updateOrderByOrderId_notFound_throwsExceptions() {
        CouponDTO forUpdate = CouponDataFactory.createCouponDTOForUpdate();

        Mockito.when(couponRepository.findById(1L))
                .thenReturn(Optional.empty());

        Assertions.assertThrows(ResponseStatusException.class,
                () -> couponServiceImpl.updateCouponByCouponId(1L, forUpdate));

        Mockito.verify(couponRepository).findById(1L);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void deleteCouponById_returnNothing() {
        Client clientWithOrders = CouponDataFactory.createPersistClientWithTwoCoupons();
        Coupon orderForDelete = CouponDataFactory.createPersistentCouponEntity();
        orderForDelete.getClients().add(clientWithOrders);

        Mockito.when(couponRepository.findById(orderForDelete.getId()))
                .thenReturn(Optional.of(orderForDelete));
        Mockito.doNothing()
                .when(couponRepository).deleteById(orderForDelete.getId());

        couponServiceImpl.deleteCouponById(orderForDelete.getId());

        Mockito.verify(couponRepository).findById(orderForDelete.getId());
        Mockito.verify(couponRepository).deleteById(orderForDelete.getId());
    }

    @Test
    void deleteCouponById_couponNotFound() {
        Mockito.when(couponRepository.findById(Mockito.anyLong()))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = Assertions.assertThrows(ResponseStatusException.class,
                () -> couponServiceImpl.deleteCouponById(999L));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        Mockito.verify(couponRepository, Mockito.times(1)).findById(999L);
        Mockito.verify(couponRepository, Mockito.never()).deleteById(Mockito.anyLong());
        Mockito.verifyNoMoreInteractions(couponRepository);
    }
}
