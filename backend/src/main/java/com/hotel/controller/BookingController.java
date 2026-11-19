package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    /** Price preview (room + extras - coupon) shown before the guest confirms. Saves nothing. */
    @PostMapping("/quote")
    public Dto.Quote quote(@Valid @RequestBody Dto.BookingRequest req) {
        return service.quote(req);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.BookingView create(Authentication auth, @Valid @RequestBody Dto.BookingRequest req) {
        return service.create(auth.getName(), req);
    }

    @GetMapping("/my")
    public List<Dto.BookingView> mine(Authentication auth) {
        return service.forUser(auth.getName());
    }

    @PutMapping("/{id}/cancel")
    public Dto.BookingView cancel(Authentication auth, @PathVariable Long id) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return service.cancel(auth.getName(), admin, id);
    }
}
