package com.hotel.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A room-service order placed by a checked-in guest. Charged to the room and paid before check-out. */
@Entity
@Table(name = "food_orders")
public class FoodOrder {
    public enum Status { PLACED, PREPARING, DELIVERED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    // SUBSELECT: loaded with its own query, so it does not clash with the booking's extras list.
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "food_order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderLine> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PLACED;

    @Column(nullable = false)
    private boolean paid = false;

    @Column(length = 200)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public List<OrderLine> getItems() { return items; }
    public void setItems(List<OrderLine> items) { this.items = items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
