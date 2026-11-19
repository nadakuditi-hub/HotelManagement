package com.hotel.repository;

import com.hotel.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    List<Coupon> findAllByOrderByIdDesc();
}
