package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rooms")
public class Room {
    public enum Type { SINGLE, DOUBLE, DELUXE, SUITE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, unique = true)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;

    @Column(nullable = false)
    private int capacity;

    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    public Room() {}

    public Room(String roomNumber, Type type, BigDecimal pricePerNight, int capacity, String description) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
