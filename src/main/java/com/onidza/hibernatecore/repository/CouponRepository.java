package com.onidza.hibernatecore.repository;

import com.onidza.hibernatecore.model.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("select cl.id from Coupon c join c.clients cl where c.id = :couponId")
    List<Long> findClientIdsByCouponId(@Param("couponId") Long couponId);
}
