package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Room-service food orders for the logged-in guest. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.OrderView create(Authentication auth, @Valid @RequestBody Dto.OrderRequest req) {
        return service.create(auth.getName(), req);
    }

    @GetMapping("/my")
    public List<Dto.OrderView> mine(Authentication auth) {
        return service.mine(auth.getName());
    }

    @PutMapping("/{id}/cancel")
    public Dto.OrderView cancel(Authentication auth, @PathVariable Long id) {
        return service.cancelByGuest(auth.getName(), id);
    }
}
