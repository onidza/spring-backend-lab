package com.onidza.hibernatecore.service.Coupon;

import com.onidza.hibernatecore.model.dto.CouponDTO;
import com.onidza.hibernatecore.model.entity.Client;
import com.onidza.hibernatecore.model.entity.Coupon;

import java.time.LocalDateTime;
import java.util.List;

public class CouponDataFactory {

    static CouponDTO createPersistentCouponDTO() {
        return new CouponDTO(
                1L,
                "NEW CODE000000",
                8.8f,
                LocalDateTime.of(2020, 1, 1, 12, 0),
                null
        );
    }

    static CouponDTO createDistinctPersistentClientDTO() {
        return new CouponDTO(
                2L,
                "NEW CODE111111",
                2.1f,
                LocalDateTime.of(2020, 1, 20, 20, 20),
                null
        );
    }

    static Coupon createPersistentCouponEntity() {
        Coupon coupon = new Coupon(
                "NEW CODE000000",
                8.8f,
                LocalDateTime.of(2020, 1, 1, 12, 0)
        );

        coupon.setId(1L);
        return coupon;
    }

    static Coupon createDistinctPersistentCouponEntity() {
        Coupon coupon = new Coupon(
                "NEW CODE111111",
                2.1f,
                LocalDateTime.of(2020, 1, 20, 20, 20)
        );

        coupon.setId(2L);
        return coupon;
    }

    static Client createPersistClientWithTwoCoupons() {
        Client client = new Client(
                "Ivan",
                "ivan-st233@mail.ru",
                null
        );
        client.setId(1L);

        Coupon firstCoupon = createPersistentCouponEntity();
        Coupon secondCoupon = createDistinctPersistentCouponEntity();

        firstCoupon.getClients().add(client);
        secondCoupon.getClients().add(client);

        client.getCoupons().add(firstCoupon);
        client.getCoupons().add(secondCoupon);

        return client;
    }

    static CouponDTO createCouponDTOForAdd() {
        return new CouponDTO(
                null,
                "NEW CODE000000",
                8.8f,
                LocalDateTime.of(2020, 1, 1, 12, 0),
                null
        );
    }

    static Coupon createPersistentCouponEntityForAdd() {
        return new Coupon(
                "NEW CODE000000",
                8.8f,
                LocalDateTime.of(2020, 1, 1, 12, 0)
        );
    }

    static CouponDTO createCouponDTOAfterUpdate() {
        return new CouponDTO(
                1L,
                "NEW CODE000000",
                8.8f,
                LocalDateTime.of(2020, 1, 1, 12, 0),
                List.of(1L)
        );
    }
}
