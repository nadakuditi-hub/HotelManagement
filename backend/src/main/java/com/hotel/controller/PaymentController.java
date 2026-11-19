package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.Booking;
import com.hotel.model.Payment;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.PaymentRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulated payment: no real gateway. Records the payment and generates an invoice number.
 * (To go real, integrate Razorpay/Stripe here.)
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentRepository payments;
    private final BookingRepository bookings;

    public PaymentController(PaymentRepository payments, BookingRepository bookings) {
        this.payments = payments;
        this.bookings = bookings;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.PaymentView pay(Authentication auth, @Valid @RequestBody Dto.PaymentRequest req) {
        Booking b = bookings.findById(req.bookingId()).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!b.getUser().getEmail().equals(auth.getName()) && !isAdmin(auth))
            throw ApiException.forbidden("Not your booking");
        if (b.getStatus() == Booking.Status.CANCELLED)
            throw ApiException.badRequest("Cannot pay for a cancelled booking");
        if (payments.existsByBookingId(b.getId()))
            throw ApiException.conflict("This booking is already paid");

        Payment p = new Payment();
        p.setBooking(b);
        p.setAmount(b.getTotalAmount());
        p.setMethod(req.method());
        p.setInvoiceNumber("INV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + ThreadLocalRandom.current().nextInt(100000, 999999));
        return Dto.PaymentView.of(payments.save(p));
    }

    @GetMapping("/booking/{bookingId}")
    public Dto.PaymentView forBooking(Authentication auth, @PathVariable Long bookingId) {
        Payment p = payments.findByBookingId(bookingId).orElseThrow(() -> ApiException.notFound("No payment for this booking"));
        if (!p.getBooking().getUser().getEmail().equals(auth.getName()) && !isAdmin(auth))
            throw ApiException.forbidden("Not your booking");
        return Dto.PaymentView.of(p);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
