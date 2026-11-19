package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** An optional extra a guest can add to a booking, e.g. breakfast or airport pickup. */
@Entity
@Table(name = "add_ons")
public class AddOn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    /** true = price is charged for every night, false = charged once per stay. */
    @Column(name = "per_night", nullable = false)
    private boolean perNight;

    @Column(nullable = false)
    private boolean active = true;

    public AddOn() {}

    public AddOn(String name, String description, BigDecimal price, boolean perNight) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.perNight = perNight;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isPerNight() { return perNight; }
    public void setPerNight(boolean perNight) { this.perNight = perNight; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
