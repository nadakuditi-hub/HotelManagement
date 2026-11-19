package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.Booking;
import com.hotel.model.Payment;
import com.hotel.model.Room;
import com.hotel.model.User;
import com.hotel.repository.*;
import com.hotel.service.BookingService;
import com.hotel.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Everything under /api/admin requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final RoomRepository rooms;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final BookingService bookingService;
    private final FileStorageService storage;

    public AdminController(RoomRepository rooms, BookingRepository bookings, PaymentRepository payments,
                           UserRepository users, BookingService bookingService, FileStorageService storage) {
        this.rooms = rooms;
        this.bookings = bookings;
        this.payments = payments;
        this.users = users;
        this.bookingService = bookingService;
        this.storage = storage;
    }

    // ---------- Dashboard ----------
    @GetMapping("/stats")
    public Dto.Stats stats() {
        long customers = users.findAll().stream().filter(u -> u.getRole() == User.Role.CUSTOMER).count();
        return new Dto.Stats(
                rooms.findByActiveTrue().size(),
                bookings.count(),
                bookings.countByStatus(Booking.Status.CONFIRMED),
                bookings.countByStatus(Booking.Status.CHECKED_IN),
                bookings.countByStatus(Booking.Status.CANCELLED),
                bookings.countOccupiedOn(LocalDate.now(), BookingService.ACTIVE),
                payments.totalRevenue(),
                customers);
    }

    // ---------- Bookings ----------
    @GetMapping("/bookings")
    public List<Dto.BookingView> allBookings() {
        return bookingService.all();
    }

    @PutMapping("/bookings/{id}/status")
    public Dto.BookingView updateStatus(@PathVariable Long id, @Valid @RequestBody Dto.StatusRequest req) {
        return bookingService.updateStatus(id, req.status());
    }

    // ---------- Payments ----------
    @GetMapping("/payments")
    public List<Dto.PaymentView> allPayments() {
        return payments.findAllByOrderByPaidAtDesc().stream().map(Dto.PaymentView::of).toList();
    }

    // ---------- Reports ----------
    /** Revenue per month for the last 12 months that have payments (refunded payments excluded). */
    @GetMapping("/reports/monthly")
    public List<Dto.MonthlyRow> monthlyReport() {
        Map<YearMonth, BigDecimal> revenue = new TreeMap<>();
        Map<YearMonth, Long> counts = new TreeMap<>();
        for (Payment p : payments.findAll()) {
            if (p.getStatus() != Payment.Status.PAID) continue;
            YearMonth ym = YearMonth.from(p.getPaidAt());
            revenue.merge(ym, p.getAmount(), BigDecimal::add);
            counts.merge(ym, 1L, Long::sum);
        }
        List<Dto.MonthlyRow> rows = new ArrayList<>();
        revenue.forEach((ym, amount) -> rows.add(new Dto.MonthlyRow(ym.toString(), counts.get(ym), amount)));
        return rows.size() > 12 ? rows.subList(rows.size() - 12, rows.size()) : rows;
    }

    @GetMapping("/export/bookings")
    public ResponseEntity<String> exportBookings() {
        StringBuilder sb = new StringBuilder("Booking ID,Guest,Email,Room,Type,Check-in,Check-out,Nights,Guests,Total (INR),Status,Paid\n");
        for (Dto.BookingView b : bookingService.all()) {
            sb.append(csv(b.id(), b.customerName(), b.customerEmail(), b.roomNumber(), b.roomType(),
                    b.checkIn(), b.checkOut(), b.nights(), b.guests(), b.totalAmount(), b.status(),
                    b.paid() ? "Yes" : "No"));
        }
        return csvResponse(sb.toString(), "bookings.csv");
    }

    @GetMapping("/export/payments")
    public ResponseEntity<String> exportPayments() {
        StringBuilder sb = new StringBuilder("Invoice,Booking ID,Guest,Room,Method,Amount (INR),Status,Paid at\n");
        for (Payment p : payments.findAllByOrderByPaidAtDesc()) {
            Dto.PaymentView v = Dto.PaymentView.of(p);
            sb.append(csv(v.invoiceNumber(), v.bookingId(), v.customerName(), v.roomNumber(), v.method(),
                    v.amount(), v.status(), v.paidAt()));
        }
        return csvResponse(sb.toString(), "payments.csv");
    }

    private static ResponseEntity<String> csvResponse(String body, String filename) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    /** Build one CSV line, quoting values that contain commas, quotes or line breaks. */
    private static String csv(Object... values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) line.append(',');
            String s = values[i] == null ? "" : String.valueOf(values[i]);
            if (s.contains(",") || s.contains("\"") || s.contains("\n")) s = "\"" + s.replace("\"", "\"\"") + "\"";
            line.append(s);
        }
        return line.append('\n').toString();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ---------- Customers ----------
    @GetMapping("/users")
    public List<User> customers() {
        return users.findAll().stream().filter(u -> u.getRole() == User.Role.CUSTOMER).toList();
    }

    // ---------- Rooms (CRUD) ----------
    @GetMapping("/rooms")
    public List<Room> allRooms() {
        return rooms.findAll();
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public Room addRoom(@Valid @RequestBody Dto.RoomRequest req) {
        if (rooms.existsByRoomNumber(req.roomNumber())) throw ApiException.conflict("Room number already exists");
        Room r = new Room(req.roomNumber().trim(), req.type(), req.pricePerNight(), req.capacity(), req.description());
        r.setImageUrl(com.hotel.util.Text.safeImageUrl(req.imageUrl()));
        r.setActive(req.active() == null || req.active());
        return rooms.save(r);
    }

    @PutMapping("/rooms/{id}")
    public Room updateRoom(@PathVariable Long id, @Valid @RequestBody Dto.RoomRequest req) {
        Room r = rooms.findById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
        if (!r.getRoomNumber().equals(req.roomNumber()) && rooms.existsByRoomNumber(req.roomNumber()))
            throw ApiException.conflict("Room number already exists");
        r.setRoomNumber(req.roomNumber().trim());
        r.setType(req.type());
        r.setPricePerNight(req.pricePerNight());
        r.setCapacity(req.capacity());
        r.setDescription(req.description());
        r.setImageUrl(com.hotel.util.Text.safeImageUrl(req.imageUrl()));
        if (req.active() != null) r.setActive(req.active());
        return rooms.save(r);
    }

    /** Upload (or replace) a room photo. Form field name: "file". JPG/PNG/WEBP, max 5 MB. */
    @PostMapping("/rooms/{id}/image")
    public Room uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Room r = rooms.findById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
        String old = r.getImageUrl();
        r.setImageUrl(storage.saveImage(file));
        Room saved = rooms.save(r);
        storage.delete(old);
        return saved;
    }

    @DeleteMapping("/rooms/{id}/image")
    public Room removeImage(@PathVariable Long id) {
        Room r = rooms.findById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
        String old = r.getImageUrl();
        r.setImageUrl(null);
        Room saved = rooms.save(r);
        storage.delete(old);
        return saved;
    }

    /** Soft delete: keeps booking history intact. */
    @DeleteMapping("/rooms/{id}")
    public Room deactivateRoom(@PathVariable Long id) {
        Room r = rooms.findById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
        r.setActive(false);
        return rooms.save(r);
    }
}
