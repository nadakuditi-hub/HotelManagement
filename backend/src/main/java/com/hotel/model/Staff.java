package com.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A hotel employee (not a login account). */
@Entity
@Table(name = "staff")
public class Staff {
    public enum Role { RECEPTIONIST, HOUSEKEEPING, MAINTENANCE, CHEF, SECURITY, MANAGER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(length = 20)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Staff() {}

    public Staff(String fullName, Role role, String phone, String email) {
        this.fullName = fullName;
        this.role = role;
        this.phone = phone;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
