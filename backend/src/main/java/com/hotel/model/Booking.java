package com.hotel.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {
    public enum Status { CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int guests;

    // Price breakdown. Nullable so bookings made before extras/coupons existed still load.
    @Column(name = "room_amount")
    private BigDecimal roomAmount;

    @Column(name = "addons_amount")
    private BigDecimal addOnsAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "coupon_code", length = 30)
    private String couponCode;

    // SUBSELECT: loaded with its own query, so several eager lists can be loaded together safely.
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "booking_addons", joinColumns = @JoinColumn(name = "booking_id"))
    private List<AddOnLine> addOns = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.CONFIRMED;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }
    public BigDecimal getRoomAmount() { return roomAmount; }
    public void setRoomAmount(BigDecimal roomAmount) { this.roomAmount = roomAmount; }
    public BigDecimal getAddOnsAmount() { return addOnsAmount; }
    public void setAddOnsAmount(BigDecimal addOnsAmount) { this.addOnsAmount = addOnsAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public List<AddOnLine> getAddOns() { return addOns; }
    public void setAddOns(List<AddOnLine> addOns) { this.addOns = addOns; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
