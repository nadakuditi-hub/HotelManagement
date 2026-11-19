package com.hotel.repository;

import com.hotel.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingId(Long bookingId);

    List<Review> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    /** One row per reviewed room: [roomId (Long), average rating (Double), count (Long)]. */
    @Query("select r.room.id, avg(r.rating), count(r) from Review r group by r.room.id")
    List<Object[]> ratingSummary();
}
