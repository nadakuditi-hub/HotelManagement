package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.User;
import com.hotel.repository.UserRepository;
import com.hotel.security.JwtUtil;
import com.hotel.service.LoginAttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final LoginAttemptService attempts;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtUtil jwt, LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.attempts = attempts;
    }

    @PostMapping("/register")
    public ResponseEntity<Dto.AuthResponse> register(@Valid @RequestBody Dto.RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw ApiException.conflict("Email is already registered");
        User u = new User();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        u.setPassword(encoder.encode(req.password()));
        u.setRole(User.Role.CUSTOMER);
        users.save(u);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Dto.AuthResponse.of(jwt.generate(u.getEmail(), u.getRole().name()), u));
    }

    @PostMapping("/login")
    public Dto.AuthResponse login(@Valid @RequestBody Dto.LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        attempts.checkAllowed(email);               // 429 if too many wrong passwords recently
        User u = users.findByEmail(email).orElse(null);
        if (u == null || !encoder.matches(req.password(), u.getPassword())) {
            attempts.recordFailure(email);          // counted even for unknown emails, so nothing is revealed
            throw new BadCredentialsException("bad");
        }
        attempts.reset(email);
        return Dto.AuthResponse.of(jwt.generate(u.getEmail(), u.getRole().name()), u);
    }
}
