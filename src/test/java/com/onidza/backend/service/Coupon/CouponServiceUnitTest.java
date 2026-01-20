package com.onidza.backend.service.Coupon;

import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;
import com.onidza.backend.model.entity.Client;
import com.onidza.backend.model.entity.Coupon;
import com.onidza.backend.model.mapper.MapperService;
import com.onidza.backend.repository.ClientRepository;
import com.onidza.backend.repository.CouponRepository;
import com.onidza.backend.service.coupon.CouponServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
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
    void getCouponById_returnCouponByDTOWithRelations() {
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
    void getCouponsPage_returnCouponsPageWithRelations() {
        Coupon persistentCouponEntity = CouponDataFactory.createPersistentCouponEntity();
        Coupon persistentDistinctCouponEntity = CouponDataFactory.createDistinctPersistentCouponEntity();

        CouponDTO persistentCouponDTO = CouponDataFactory.createPersistentCouponDTO();
        CouponDTO persistentDistinctCouponDTO = CouponDataFactory.createDistinctPersistentCouponDTO();

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Coupon> pageFromRepo = new PageImpl<>(
                List.of(persistentCouponEntity, persistentDistinctCouponEntity),
                pageable,
                2
        );

        Mockito.when(couponRepository.findAll(pageable))
                .thenReturn(pageFromRepo);
        Mockito.when(mapperService.couponToDTO(persistentCouponEntity))
                .thenReturn(persistentCouponDTO);
        Mockito.when(mapperService.couponToDTO(persistentDistinctCouponEntity))
                .thenReturn(persistentDistinctCouponDTO);

        CouponPageDTO page = couponServiceImpl.getCouponsPage(0, 20);

        Assertions.assertNotNull(page);
        Assertions.assertEquals(2, page.items().size());
        Assertions.assertEquals(0, page.page());
        Assertions.assertEquals(20, page.size());
        Assertions.assertFalse(page.hasNext());

        Assertions.assertTrue(page.items().stream().anyMatch(c -> c.id().equals(1L)));
        Assertions.assertTrue(page.items().stream().anyMatch(c -> c.id().equals(2L)));

        Mockito.verify(couponRepository).findAll(pageable);
        Mockito.verify(mapperService).couponToDTO(persistentCouponEntity);
        Mockito.verify(mapperService).couponToDTO(persistentDistinctCouponEntity);
        Mockito.verifyNoMoreInteractions(couponRepository, mapperService);
    }

    @Test
    void getCoupons_Page_emptyList() {
        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Coupon> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                pageable,
                0
        );

        Mockito.when(couponRepository.findAll(pageable))
                .thenReturn(emptyPage);

        CouponPageDTO result = couponServiceImpl.getCouponsPage(0, 20);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.items().isEmpty());
        Assertions.assertFalse(result.hasNext());
        Assertions.assertEquals(0, result.page());
        Assertions.assertEquals(20, result.size());

        Mockito.verify(couponRepository).findAll(pageable);
        Mockito.verifyNoInteractions(mapperService);
    }

    @Test
    void getCouponsPageByClientId_returnPageDTOWithRelations() {
        Long clientId = 1L;

        Coupon persistentCouponEntity = CouponDataFactory.createPersistentCouponEntity();
        Coupon persistentDistinctCouponEntity = CouponDataFactory.createDistinctPersistentCouponEntity();

        CouponDTO persistentCouponDTO = CouponDataFactory.createPersistentCouponDTO();
        CouponDTO persistentDistinctCouponDTO = CouponDataFactory.createDistinctPersistentCouponDTO();


        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Slice<Coupon> slice = new SliceImpl<>(List.of(
                persistentCouponEntity,
                persistentDistinctCouponEntity),
                pageable,
                false);

        Mockito.when(couponRepository.findByClientsId(clientId, pageable))
                .thenReturn(slice);
        Mockito.when(mapperService.couponToDTO(persistentCouponEntity))
                .thenReturn(persistentCouponDTO);
        Mockito.when(mapperService.couponToDTO(persistentDistinctCouponEntity))
                .thenReturn(persistentDistinctCouponDTO);

        CouponPageDTO page = couponServiceImpl.getCouponsPageByClientId(clientId, 0, 20);

        Assertions.assertNotNull(page);
        Assertions.assertEquals(2, page.items().size());
        Assertions.assertFalse(page.hasNext());

        Assertions.assertTrue(page.items().stream()
                .anyMatch(c -> c.id().equals(1L)));

        Assertions.assertTrue(page.items().stream()
                .anyMatch(c -> c.id().equals(2L)));

        Assertions.assertTrue(page.items().stream()
                .anyMatch(c -> c.discount() == 8.8f));

        Mockito.verify(couponRepository)
                .findByClientsId(clientId, pageable);

        Mockito.verify(mapperService, Mockito.times(2))
                .couponToDTO(Mockito.any(Coupon.class));

        Mockito.verifyNoMoreInteractions(
                clientRepository,
                couponRepository,
                mapperService
        );
    }

    @Test
    void getCouponsPageByClientId_notFound_throwsException() {
        Long clientId = 1L;

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        Page<Coupon> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                pageable,
                0
        );

        Mockito.when(couponRepository.findByClientsId(clientId, pageable))
                .thenReturn(emptyPage);

        CouponPageDTO result =
                couponServiceImpl.getCouponsPageByClientId(clientId, 0, 20);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.items().isEmpty());
        Assertions.assertFalse(result.hasNext());
        Assertions.assertEquals(0, result.page());
        Assertions.assertEquals(20, result.size());

        Mockito.verify(couponRepository)
                .findByClientsId(clientId, pageable);

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

        CouponDTO result = couponServiceImpl.addCouponToClientByClientId(client.getId(), couponDTOForAdd);

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
                () -> couponServiceImpl.addCouponToClientByClientId(1L, couponDTOForAdd));

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
    void deleteCouponByCouponId_returnNothing() {
        Client clientWithOrders = CouponDataFactory.createPersistClientWithTwoCoupons();
        Coupon orderForDelete = CouponDataFactory.createPersistentCouponEntity();
        orderForDelete.getClients().add(clientWithOrders);

        Mockito.when(couponRepository.findById(orderForDelete.getId()))
                .thenReturn(Optional.of(orderForDelete));
        Mockito.doNothing()
                .when(couponRepository).deleteById(orderForDelete.getId());

        couponServiceImpl.deleteCouponByCouponId(orderForDelete.getId());

        Mockito.verify(couponRepository).findById(orderForDelete.getId());
        Mockito.verify(couponRepository).deleteById(orderForDelete.getId());
    }

    @Test
    void deleteCouponByCouponId_couponNotFound() {
        Mockito.when(couponRepository.findById(Mockito.anyLong()))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = Assertions.assertThrows(ResponseStatusException.class,
                () -> couponServiceImpl.deleteCouponByCouponId(999L));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        Mockito.verify(couponRepository, Mockito.times(1)).findById(999L);
        Mockito.verify(couponRepository, Mockito.never()).deleteById(Mockito.anyLong());
        Mockito.verifyNoMoreInteractions(couponRepository);
    }
}
