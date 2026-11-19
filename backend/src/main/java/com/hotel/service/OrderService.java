package com.hotel.service;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.Booking;
import com.hotel.model.FoodOrder;
import com.hotel.model.MenuItem;
import com.hotel.model.OrderLine;
import com.hotel.model.User;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.FoodOrderRepository;
import com.hotel.repository.MenuItemRepository;
import com.hotel.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Room-service food orders: placed by checked-in guests, charged to the room, paid before check-out. */
@Service
public class OrderService {
    private final FoodOrderRepository orders;
    private final BookingRepository bookings;
    private final MenuItemRepository menu;
    private final UserRepository users;

    public OrderService(FoodOrderRepository orders, BookingRepository bookings,
                        MenuItemRepository menu, UserRepository users) {
        this.orders = orders;
        this.bookings = bookings;
        this.menu = menu;
        this.users = users;
    }

    @Transactional
    public Dto.OrderView create(String email, Dto.OrderRequest req) {
        Booking b = bookings.findById(req.bookingId()).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!b.getUser().getEmail().equals(email)) throw ApiException.forbidden("This is not your booking");
        if (b.getStatus() != Booking.Status.CHECKED_IN)
            throw ApiException.badRequest("Room service is available only during your stay, after check-in");

        // Merge repeated items so quantity limits apply per dish.
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (Dto.OrderItemRequest i : req.items()) quantities.merge(i.menuItemId(), i.quantity(), Integer::sum);

        FoodOrder order = new FoodOrder();
        order.setBooking(b);
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
            MenuItem item = menu.findById(e.getKey()).filter(MenuItem::isAvailable)
                    .orElseThrow(() -> ApiException.badRequest("An item in your order is no longer available"));
            if (e.getValue() > 10) throw ApiException.badRequest("You can order at most 10 of one item");
            order.getItems().add(new OrderLine(item.getName(), item.getPrice(), e.getValue()));
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(e.getValue())));
        }
        order.setTotalAmount(total);
        String notes = req.notes() == null ? null : req.notes().trim();
        order.setNotes(notes == null || notes.isEmpty() ? null : notes);
        return Dto.OrderView.of(orders.save(order));
    }

    public List<Dto.OrderView> mine(String email) {
        User u = users.findByEmail(email).orElseThrow(() -> ApiException.notFound("User not found"));
        return orders.findByBookingUserIdOrderByCreatedAtDesc(u.getId()).stream().map(Dto.OrderView::of).toList();
    }

    public List<Dto.OrderView> all() {
        return orders.findAllByOrderByCreatedAtDesc().stream().map(Dto.OrderView::of).toList();
    }

    /** A guest can cancel an order only while it is still PLACED (the kitchen has not started). */
    @Transactional
    public Dto.OrderView cancelByGuest(String email, Long id) {
        FoodOrder o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (!o.getBooking().getUser().getEmail().equals(email)) throw ApiException.forbidden("This is not your order");
        if (o.getStatus() != FoodOrder.Status.PLACED)
            throw ApiException.badRequest("The kitchen has already started this order, so it cannot be cancelled");
        o.setStatus(FoodOrder.Status.CANCELLED);
        return Dto.OrderView.of(orders.save(o));
    }

    /** Admin: PLACED -> PREPARING -> DELIVERED, or cancel while not yet delivered. */
    @Transactional
    public Dto.OrderView updateStatus(Long id, FoodOrder.Status next) {
        FoodOrder o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        FoodOrder.Status cur = o.getStatus();
        boolean ok = (cur == FoodOrder.Status.PLACED && (next == FoodOrder.Status.PREPARING || next == FoodOrder.Status.CANCELLED))
                || (cur == FoodOrder.Status.PREPARING && (next == FoodOrder.Status.DELIVERED || next == FoodOrder.Status.CANCELLED));
        if (!ok) throw ApiException.badRequest("Cannot change an order from " + cur + " to " + next);
        o.setStatus(next);
        return Dto.OrderView.of(orders.save(o));
    }

    @Transactional
    public Dto.OrderView markPaid(Long id) {
        FoodOrder o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
        if (o.getStatus() == FoodOrder.Status.CANCELLED) throw ApiException.badRequest("A cancelled order cannot be paid");
        if (o.isPaid()) throw ApiException.conflict("This order is already marked as paid");
        o.setPaid(true);
        return Dto.OrderView.of(orders.save(o));
    }
}
