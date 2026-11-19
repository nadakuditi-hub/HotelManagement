package com.hotel.repository;

import com.hotel.model.Booking;
import com.hotel.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByRoomNumber(String roomNumber);

    List<Room> findByActiveTrue();

    /** Active rooms with no overlapping non-cancelled booking in [checkIn, checkOut). */
    @Query("""
            select r from Room r
            where r.active = true
              and r.id not in (
                select b.room.id from Booking b
                where b.status in :statuses
                  and b.checkIn < :checkOut
                  and b.checkOut > :checkIn)
            order by r.pricePerNight
            """)
    List<Room> findAvailable(@Param("checkIn") LocalDate checkIn,
                             @Param("checkOut") LocalDate checkOut,
                             @Param("statuses") List<Booking.Status> statuses);
}
