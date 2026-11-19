package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.User;
import com.hotel.repository.UserRepository;
import com.hotel.service.LoginAttemptService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** The logged-in user's own account. */
@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final LoginAttemptService attempts;

    public AccountController(UserRepository users, PasswordEncoder encoder, LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.attempts = attempts;
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(Authentication auth, @Valid @RequestBody Dto.ChangePasswordRequest req) {
        String email = auth.getName();
        attempts.checkAllowed(email);
        User u = users.findByEmail(email).orElseThrow(() -> ApiException.notFound("User not found"));
        if (!encoder.matches(req.currentPassword(), u.getPassword())) {
            attempts.recordFailure(email);
            throw ApiException.badRequest("Current password is incorrect");
        }
        if (encoder.matches(req.newPassword(), u.getPassword()))
            throw ApiException.badRequest("New password must be different from the current one");
        u.setPassword(encoder.encode(req.newPassword()));
        users.save(u);
        attempts.reset(email);
        return Map.of("message", "Password changed successfully");
    }
}
