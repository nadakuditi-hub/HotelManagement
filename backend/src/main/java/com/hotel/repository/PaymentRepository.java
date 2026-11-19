package com.hotel.repository;

import com.hotel.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    List<Payment> findAllByOrderByPaidAtDesc();

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = :status")
    BigDecimal sumByStatus(@Param("status") Payment.Status status);

    /** Total money received (refunded payments are excluded). */
    default BigDecimal totalRevenue() {
        return sumByStatus(Payment.Status.PAID);
    }
}
