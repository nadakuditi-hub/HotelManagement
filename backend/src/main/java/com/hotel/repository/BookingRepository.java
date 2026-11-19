package com.hotel.repository;

import com.hotel.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findAllByOrderByCreatedAtDesc();

    long countByStatus(Booking.Status status);

    @Query("""
            select count(b) from Booking b
            where b.room.id = :roomId
              and b.status in :statuses
              and b.checkIn < :checkOut
              and b.checkOut > :checkIn
            """)
    long countOverlapping(@Param("roomId") Long roomId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut,
                          @Param("statuses") List<Booking.Status> statuses);

    /** Rooms occupied on a given date. */
    @Query("""
            select count(distinct b.room.id) from Booking b
            where b.status in :statuses
              and b.checkIn <= :day
              and b.checkOut > :day
            """)
    long countOccupiedOn(@Param("day") LocalDate day,
                         @Param("statuses") List<Booking.Status> statuses);
}
