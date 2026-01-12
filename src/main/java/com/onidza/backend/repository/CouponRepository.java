package com.onidza.backend.repository;

import com.onidza.backend.model.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("""
            SELECT cl.id
            FROM Coupon c
            JOIN c.clients cl
            WHERE c.id = :couponId
            """)
    List<Long> findClientIdsByCouponId(@Param("couponId") Long couponId);

    Page<Coupon> findByClientsId(Long id, Pageable pageable);
}
