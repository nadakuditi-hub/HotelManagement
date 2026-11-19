package com.hotel.service;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.*;
import com.hotel.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {
    /** Booking statuses that block a room for the given dates. */
    public static final List<Booking.Status> ACTIVE =
            List.of(Booking.Status.CONFIRMED, Booking.Status.CHECKED_IN);

    private static final int MAX_NIGHTS = 30;
    private static final int MAX_ADVANCE_DAYS = 365;

    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final UserRepository users;
    private final PaymentRepository payments;
    private final ReviewRepository reviews;
    private final PricingService pricing;
    private final CouponRepository coupons;
    private final HousekeepingTaskRepository tasks;
    private final FoodOrderRepository foodOrders;

    public BookingService(BookingRepository bookings, RoomRepository rooms, UserRepository users,
                          PaymentRepository payments, ReviewRepository reviews, PricingService pricing,
                          CouponRepository coupons, HousekeepingTaskRepository tasks,
                          FoodOrderRepository foodOrders) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.users = users;
        this.payments = payments;
        this.reviews = reviews;
        this.pricing = pricing;
        this.coupons = coupons;
        this.tasks = tasks;
        this.foodOrders = foodOrders;
    }

    public Dto.BookingView view(Booking b) {
        return Dto.BookingView.of(b, payments.existsByBookingId(b.getId()), reviews.existsByBookingId(b.getId()));
    }

    /** Checks the request and returns the room (shared by quote and create). */
    private Room checkRequest(Dto.BookingRequest req) {
        Room room = rooms.findById(req.roomId()).orElseThrow(() -> ApiException.notFound("Room not found"));
        LocalDate today = LocalDate.now();
        if (!room.isActive()) throw ApiException.badRequest("This room is not available for booking");
        if (req.checkIn().isBefore(today)) throw ApiException.badRequest("Check-in cannot be in the past");
        if (!req.checkOut().isAfter(req.checkIn())) throw ApiException.badRequest("Check-out must be after check-in");
        if (ChronoUnit.DAYS.between(req.checkIn(), req.checkOut()) > MAX_NIGHTS)
            throw ApiException.badRequest("You can book at most " + MAX_NIGHTS + " nights at a time");
        if (req.checkIn().isAfter(today.plusDays(MAX_ADVANCE_DAYS)))
            throw ApiException.badRequest("Bookings can be made up to one year in advance");
        if (req.guests() > room.getCapacity())
            throw ApiException.badRequest("This room allows at most " + room.getCapacity() + " guest(s)");
        if (bookings.countOverlapping(room.getId(), req.checkIn(), req.checkOut(), ACTIVE) > 0)
            throw ApiException.conflict("Room is already booked for the selected dates");
        return room;
    }

    /** Price preview shown in the booking window (nothing is saved). */
    public Dto.Quote quote(Dto.BookingRequest req) {
        Room room = checkRequest(req);
        return pricing.price(room, req.checkIn(), req.checkOut(), req.addOnIds(), req.couponCode(), false).quote();
    }

    @Transactional
    public Dto.BookingView create(String email, Dto.BookingRequest req) {
        User user = users.findByEmail(email).orElseThrow(() -> ApiException.notFound("User not found"));
        Room room = checkRequest(req);
        PricingService.Priced priced =
                pricing.price(room, req.checkIn(), req.checkOut(), req.addOnIds(), req.couponCode(), true);
        Dto.Quote q = priced.quote();

        Booking b = new Booking();
        b.setUser(user);
        b.setRoom(room);
        b.setCheckIn(req.checkIn());
        b.setCheckOut(req.checkOut());
        b.setGuests(req.guests());
        b.setRoomAmount(q.roomAmount());
        b.setAddOnsAmount(q.addOnsAmount());
        b.setDiscountAmount(q.discount());
        b.setCouponCode(q.couponCode());
        b.getAddOns().addAll(q.addOns());
        b.setTotalAmount(q.total());
        b.setStatus(Booking.Status.CONFIRMED);

        if (priced.coupon() != null) {
            Coupon c = priced.coupon();
            c.setUsedCount(c.getUsedCount() + 1);
            coupons.save(c);
        }
        return view(bookings.save(b));
    }

    public List<Dto.BookingView> forUser(String email) {
        User user = users.findByEmail(email).orElseThrow(() -> ApiException.notFound("User not found"));
        return bookings.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::view).toList();
    }

    public List<Dto.BookingView> all() {
        return bookings.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();
    }

    @Transactional
    public Dto.BookingView cancel(String email, boolean isAdmin, Long id) {
        Booking b = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!isAdmin && !b.getUser().getEmail().equals(email))
            throw ApiException.forbidden("You can only cancel your own bookings");
        if (b.getStatus() != Booking.Status.CONFIRMED)
            throw ApiException.badRequest("Only confirmed bookings can be cancelled");
        b.setStatus(Booking.Status.CANCELLED);
        payments.findByBookingId(id).ifPresent(p -> {
            p.setStatus(Payment.Status.REFUNDED);
            payments.save(p);
        });
        // Give the coupon use back so the offer is not wasted.
        if (b.getCouponCode() != null) {
            coupons.findByCode(b.getCouponCode()).ifPresent(c -> {
                c.setUsedCount(Math.max(0, c.getUsedCount() - 1));
                coupons.save(c);
            });
        }
        return view(bookings.save(b));
    }

    /** Admin: move a booking through CONFIRMED -> CHECKED_IN -> CHECKED_OUT. */
    @Transactional
    public Dto.BookingView updateStatus(Long id, Booking.Status next) {
        Booking b = bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
        Booking.Status cur = b.getStatus();
        boolean ok = (cur == Booking.Status.CONFIRMED && next == Booking.Status.CHECKED_IN)
                || (cur == Booking.Status.CHECKED_IN && next == Booking.Status.CHECKED_OUT);
        if (!ok) throw ApiException.badRequest("Cannot change status from " + cur + " to " + next);

        if (next == Booking.Status.CHECKED_IN
                && tasks.existsByRoomIdAndStatusNot(b.getRoom().getId(), HousekeepingTask.Status.DONE)) {
            throw ApiException.badRequest("Room " + b.getRoom().getRoomNumber()
                    + " is not ready yet. Finish its pending housekeeping task first (Admin > Housekeeping).");
        }

        if (next == Booking.Status.CHECKED_OUT) {
            if (!payments.existsByBookingId(id))
                throw ApiException.badRequest("Payment must be completed before check-out");
            BigDecimal unpaidFood = foodOrders.findByBookingId(id).stream()
                    .filter(o -> o.getStatus() != FoodOrder.Status.CANCELLED && !o.isPaid())
                    .map(FoodOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (unpaidFood.signum() > 0)
                throw ApiException.badRequest("Food orders worth Rs " + unpaidFood.stripTrailingZeros().toPlainString()
                        + " are unpaid. Collect the payment first (Admin > Food orders).");
        }

        b.setStatus(next);
        Booking saved = bookings.save(b);
        if (next == Booking.Status.CHECKED_OUT) {
            tasks.save(new HousekeepingTask(b.getRoom(), HousekeepingTask.Type.CLEANING,
                    "Check-out of booking #" + id));
        }
        return view(saved);
    }
}
