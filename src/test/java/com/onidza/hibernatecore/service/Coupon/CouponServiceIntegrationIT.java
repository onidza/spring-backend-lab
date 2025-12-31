package com.onidza.hibernatecore.service.Coupon;

import com.onidza.hibernatecore.model.dto.ClientDTO;
import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.service.CouponServiceImpl;
import com.onidza.hibernatecore.service.client.ClientServiceImpl;
import com.onidza.hibernatecore.service.testcontainers.AbstractITConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
class CouponServiceIntegrationIT extends AbstractITConfiguration {

    @Autowired
    private CouponServiceImpl couponServiceImpl;

    @Autowired
    private ClientServiceImpl clientServiceImpl;

    @Test
    void getCouponById_returnCouponDTOWithRelations() {
        ClientDTO inputClientDTO = CouponDataFactory.createClientDTOWithOneCoupon();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        CouponDTO result = couponServiceImpl.getCouponById(saved.coupons().get(0).id());

        Assertions.assertEquals(saved.coupons().get(0).expirationDate(), result.expirationDate());
        Assertions.assertEquals(saved.coupons().get(0).code(), result.code());
        Assertions.assertEquals(saved.coupons().get(0).id(), result.id());
    }

    @Test
    void getAllCoupons_returnListOfCouponsDTOWithRelations() {
        ClientDTO inputClientDTO = CouponDataFactory.createClientDTOWithOneCoupon();
        ClientDTO distinctInputClientDTO = CouponDataFactory.createDistinctClientDTOWithOneCoupon();

        clientServiceImpl.addClient(inputClientDTO);
        clientServiceImpl.addClient(distinctInputClientDTO);

        List<CouponDTO> result = couponServiceImpl.getAllCoupons();

        Assertions.assertEquals(2, result.size());

        Assertions.assertTrue(result.stream()
                .anyMatch(o -> o.discount() == 2.1f)
        );

        Assertions.assertTrue(result.stream()
                .anyMatch(o -> o.discount() == 8.8f)
        );

        Set<Float> statuses = result.stream().map(CouponDTO::discount).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(8.8f, 2.1f), statuses);
    }

    @Test
    void updateOrderById_returnUpdatedOrderDTOWithRelations() {
        ClientDTO inputClientDTO = CouponDataFactory.createClientDTOWithOneCoupon();
        CouponDTO forUpdate = CouponDataFactory.createCouponDTOForUpdate();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        CouponDTO result = couponServiceImpl.updateCouponByCouponId(saved.coupons().get(0).id(), forUpdate);

        Assertions.assertEquals(saved.coupons().get(0).id(), result.id());
        Assertions.assertNotEquals(saved.coupons().get(0).discount(), result.discount());
        Assertions.assertTrue(result.expirationDate().isAfter(saved.coupons().get(0).expirationDate()));

        CouponDTO fetched = couponServiceImpl.getCouponById(result.id());
        Assertions.assertEquals("NEW CODE111111", fetched.code());
        Assertions.assertEquals(2.1f, fetched.discount());
    }

    @Test
    void addOrderToClient_returnOrderDTOWithRelations() {
        ClientDTO inputClientDTO = CouponDataFactory.createInputClientDTOWithEmptyCoupons();
        CouponDTO couponDTOForAdd = CouponDataFactory.createCouponDTOForAdd();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        CouponDTO result = couponServiceImpl.addCouponToClientById(saved.id(), couponDTOForAdd);

        Assertions.assertEquals(saved.id(), result.clientsId().get(0));
        Assertions.assertEquals(couponDTOForAdd.code(), result.code());
        Assertions.assertEquals(couponDTOForAdd.discount(), result.discount());
        Assertions.assertEquals(couponDTOForAdd.expirationDate(), result.expirationDate());

        ClientDTO featured = clientServiceImpl.getClientById(saved.id());
        Assertions.assertEquals(1, featured.coupons().size());
        Assertions.assertEquals(result.id(), featured.coupons().get(0).id());
    }

    @Test
    void deleteCouponById_returnNothingWithRelations() {
        ClientDTO inputCouponDTO = CouponDataFactory.createClientDTOWithOneCoupon();

        ClientDTO saved = clientServiceImpl.addClient(inputCouponDTO);
        couponServiceImpl.deleteCouponById(saved.coupons().get(0).id());

        Executable exec = () -> couponServiceImpl.getCouponById(saved.coupons().get(0).id());
        Assertions.assertThrows(ResponseStatusException.class, exec);

        List<CouponDTO> coupons = couponServiceImpl.getAllCoupons();
        Assertions.assertTrue(coupons.stream().noneMatch(o -> o.id().equals(saved.coupons().get(0).id())));
    }
}
