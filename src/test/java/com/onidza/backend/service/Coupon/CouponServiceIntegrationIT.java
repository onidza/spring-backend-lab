package com.onidza.backend.service.Coupon;

import com.onidza.backend.model.dto.client.ClientDTO;
import com.onidza.backend.model.dto.coupon.CouponDTO;
import com.onidza.backend.model.dto.coupon.CouponPageDTO;
import com.onidza.backend.service.client.ClientServiceImpl;
import com.onidza.backend.service.coupon.CouponServiceImpl;
import com.onidza.backend.service.testcontainers.AbstractITConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Transactional
class CouponServiceIntegrationIT extends AbstractITConfiguration {

    @Autowired
    private CouponServiceImpl couponServiceImpl;

    @Autowired
    private ClientServiceImpl clientServiceImpl;

    @Test
    void getCouponByCouponId_returnCouponDTOWithRelations() {
        ClientDTO inputClientDTO = CouponDataFactory.createClientDTOWithOneCoupon();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        CouponDTO result = couponServiceImpl.getCouponByCouponId(saved.coupons().get(0).id());

        Assertions.assertEquals(saved.coupons().get(0).expirationDate(), result.expirationDate());
        Assertions.assertEquals(saved.coupons().get(0).code(), result.code());
        Assertions.assertEquals(saved.coupons().get(0).id(), result.id());
    }

    @Test
    void getCouponsPage_returnPageDTOWithRelations() {
        ClientDTO inputClientDTO = CouponDataFactory.createClientDTOWithOneCoupon();
        ClientDTO distinctInputClientDTO = CouponDataFactory.createDistinctClientDTOWithOneCoupon();

        clientServiceImpl.addClient(inputClientDTO);
        clientServiceImpl.addClient(distinctInputClientDTO);

        CouponPageDTO page = couponServiceImpl.getCouponsPage(0, 20);

        Assertions.assertEquals(2, page.items().size());

        Assertions.assertTrue(page.items().stream()
                .anyMatch(o -> o.discount() == 2.1f)
        );

        Assertions.assertTrue(page.items().stream()
                .anyMatch(o -> o.discount() == 8.8f)
        );

        Set<Float> statuses = page.items().stream().map(CouponDTO::discount).collect(Collectors.toSet());
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

        CouponDTO fetched = couponServiceImpl.getCouponByCouponId(result.id());
        Assertions.assertEquals("NEW CODE111111", fetched.code());
        Assertions.assertEquals(2.1f, fetched.discount());
    }

    @Test
    void addOrderToClient_returnOrderDTOWithRelations() {
        ClientDTO inputClientDTO = CouponDataFactory.createInputClientDTOWithEmptyCoupons();
        CouponDTO couponDTOForAdd = CouponDataFactory.createCouponDTOForAdd();

        ClientDTO saved = clientServiceImpl.addClient(inputClientDTO);
        CouponDTO result = couponServiceImpl.addCouponToClientByClientId(saved.id(), couponDTOForAdd);

        Assertions.assertEquals(saved.id(), result.clientsId().get(0));
        Assertions.assertEquals(couponDTOForAdd.code(), result.code());
        Assertions.assertEquals(couponDTOForAdd.discount(), result.discount());
        Assertions.assertEquals(couponDTOForAdd.expirationDate(), result.expirationDate());

        ClientDTO featured = clientServiceImpl.getClientById(saved.id());
        Assertions.assertEquals(1, featured.coupons().size());
        Assertions.assertEquals(result.id(), featured.coupons().get(0).id());
    }

    @Test
    void deleteCouponByCouponId_returnNothingWithRelations() {
        ClientDTO inputCouponDTO = CouponDataFactory.createClientDTOWithOneCoupon();

        ClientDTO saved = clientServiceImpl.addClient(inputCouponDTO);
        couponServiceImpl.deleteCouponByCouponId(saved.coupons().get(0).id());

        Executable exec = () -> couponServiceImpl.getCouponByCouponId(saved.coupons().get(0).id());
        Assertions.assertThrows(ResponseStatusException.class, exec);

        CouponPageDTO page = couponServiceImpl.getCouponsPage(0, 20);
        Assertions.assertTrue(page.items().stream().noneMatch(o -> o.id().equals(saved.coupons().get(0).id())));
    }
}
