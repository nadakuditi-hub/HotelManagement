package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.Booking;
import com.hotel.model.Review;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.ReviewRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Guests review a room after check-out. Reading reviews is public: GET /api/rooms/{id}/reviews. */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewRepository reviews;
    private final BookingRepository bookings;

    public ReviewController(ReviewRepository reviews, BookingRepository bookings) {
        this.reviews = reviews;
        this.bookings = bookings;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.ReviewView create(Authentication auth, @Valid @RequestBody Dto.ReviewRequest req) {
        Booking b = bookings.findById(req.bookingId()).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!b.getUser().getEmail().equals(auth.getName()))
            throw ApiException.forbidden("You can only review your own stays");
        if (b.getStatus() != Booking.Status.CHECKED_OUT)
            throw ApiException.badRequest("You can review a room only after check-out");
        if (reviews.existsByBookingId(b.getId()))
            throw ApiException.conflict("You have already reviewed this stay");

        Review r = new Review();
        r.setBooking(b);
        r.setUser(b.getUser());
        r.setRoom(b.getRoom());
        r.setRating(req.rating());
        String comment = req.comment() == null ? null : req.comment().trim();
        r.setComment(comment == null || comment.isEmpty() ? null : comment);
        return Dto.ReviewView.of(reviews.save(r));
    }
}
