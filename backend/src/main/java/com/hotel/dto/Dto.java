package com.hotel.dto;

import com.hotel.model.AddOnLine;
import com.hotel.model.Booking;
import com.hotel.model.Coupon;
import com.hotel.model.FoodOrder;
import com.hotel.model.HousekeepingTask;
import com.hotel.model.MenuItem;
import com.hotel.model.OrderLine;
import com.hotel.model.Payment;
import com.hotel.model.Review;
import com.hotel.model.Room;
import com.hotel.model.Staff;
import com.hotel.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** All request/response shapes live here to keep the project small. */
public final class Dto {
    private Dto() {}

    // Shared validation rules
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    public static final String PASSWORD_MSG = "Password must be 8 to 64 characters and include at least one letter and one number";
    public static final String PHONE_REGEX = "^$|^[0-9+\\- ]{7,15}$";
    public static final String PHONE_MSG = "Enter a valid phone number (digits, +, - and spaces only)";

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    // ---------- Auth ----------
    public record RegisterRequest(
            @NotBlank(message = "Full name is required") @Size(max = 100, message = "Name is too long") String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Invalid email") @Size(max = 120, message = "Email is too long") String email,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG) String phone,
            @NotBlank(message = "Password is required") @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String password) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required") @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String newPassword) {}

    public record AuthResponse(String token, Long id, String fullName, String email, String role) {
        public static AuthResponse of(String token, User u) {
            return new AuthResponse(token, u.getId(), u.getFullName(), u.getEmail(), u.getRole().name());
        }
    }

    // ---------- Rooms ----------
    public record RoomRequest(
            @NotBlank(message = "Room number is required") @Size(max = 10, message = "Room number is too long") String roomNumber,
            @NotNull(message = "Room type is required") Room.Type type,
            @NotNull(message = "Price is required") @DecimalMin(value = "1", message = "Price must be positive") BigDecimal pricePerNight,
            @Min(value = 1, message = "Capacity must be at least 1") @Max(value = 10, message = "Capacity can be at most 10") int capacity,
            @Size(max = 500, message = "Description is too long") String description,
            @Size(max = 500, message = "Image link is too long") String imageUrl,
            Boolean active) {}

    /** Room as shown to the public: includes average rating and number of reviews. */
    public record RoomView(Long id, String roomNumber, String type, BigDecimal pricePerNight,
                           int capacity, String description, boolean active, String imageUrl,
                           Double avgRating, long reviewCount) {
        public static RoomView of(Room r, Double avg, long count) {
            Double rounded = avg == null ? null : Math.round(avg * 10.0) / 10.0;
            return new RoomView(r.getId(), r.getRoomNumber(), r.getType().name(), r.getPricePerNight(),
                    r.getCapacity(), r.getDescription(), r.isActive(), r.getImageUrl(), rounded, count);
        }
    }

    // ---------- Reviews ----------
    public record ReviewRequest(
            @NotNull(message = "Booking is required") Long bookingId,
            @Min(value = 1, message = "Rating must be between 1 and 5")
            @Max(value = 5, message = "Rating must be between 1 and 5") int rating,
            @Size(max = 500, message = "Comment can be at most 500 characters") String comment) {}

    public record ReviewView(Long id, int rating, String comment, String customerName,
                             String roomNumber, LocalDateTime createdAt) {
        public static ReviewView of(Review r) {
            return new ReviewView(r.getId(), r.getRating(), r.getComment(), r.getUser().getFullName(),
                    r.getRoom().getRoomNumber(), r.getCreatedAt());
        }
    }

    // ---------- Bookings ----------
    public record BookingRequest(
            @NotNull(message = "Room is required") Long roomId,
            @NotNull(message = "Check-in date is required") LocalDate checkIn,
            @NotNull(message = "Check-out date is required") LocalDate checkOut,
            @Min(value = 1, message = "At least 1 guest") int guests,
            @Size(max = 10, message = "Too many extras selected") List<Long> addOnIds,
            @Size(max = 30, message = "Coupon code is too long") String couponCode) {}

    public record StatusRequest(@NotNull(message = "Status is required") Booking.Status status) {}

    /** Price preview or final price: room + extras - coupon discount. */
    public record Quote(long nights, BigDecimal roomAmount, List<AddOnLine> addOns, BigDecimal addOnsAmount,
                        BigDecimal discount, String couponCode, String couponError, BigDecimal total) {}

    public record BookingView(Long id, Long roomId, String roomNumber, String roomType,
                              Long customerId, String customerName, String customerEmail,
                              LocalDate checkIn, LocalDate checkOut, long nights, int guests,
                              BigDecimal roomAmount, BigDecimal addOnsAmount, BigDecimal discountAmount,
                              String couponCode, List<AddOnLine> addOns,
                              BigDecimal totalAmount, String status, boolean paid, boolean reviewed,
                              LocalDateTime createdAt) {
        public static BookingView of(Booking b, boolean paid, boolean reviewed) {
            long nights = java.time.temporal.ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut());
            BigDecimal roomAmount = b.getRoomAmount() != null ? b.getRoomAmount() : b.getTotalAmount();
            return new BookingView(b.getId(), b.getRoom().getId(), b.getRoom().getRoomNumber(),
                    b.getRoom().getType().name(), b.getUser().getId(), b.getUser().getFullName(),
                    b.getUser().getEmail(), b.getCheckIn(), b.getCheckOut(), nights, b.getGuests(),
                    roomAmount, nz(b.getAddOnsAmount()), nz(b.getDiscountAmount()),
                    b.getCouponCode(), List.copyOf(b.getAddOns()),
                    b.getTotalAmount(), b.getStatus().name(), paid, reviewed, b.getCreatedAt());
        }
    }

    // ---------- Payments ----------
    public record PaymentRequest(
            @NotNull(message = "Booking is required") Long bookingId,
            @NotNull(message = "Payment method is required") Payment.Method method) {}

    public record PaymentView(Long id, Long bookingId, String invoiceNumber, BigDecimal amount,
                              String method, String status, LocalDateTime paidAt,
                              String customerName, String roomNumber,
                              LocalDate checkIn, LocalDate checkOut) {
        public static PaymentView of(Payment p) {
            Booking b = p.getBooking();
            return new PaymentView(p.getId(), b.getId(), p.getInvoiceNumber(), p.getAmount(),
                    p.getMethod().name(), p.getStatus().name(), p.getPaidAt(),
                    b.getUser().getFullName(), b.getRoom().getRoomNumber(),
                    b.getCheckIn(), b.getCheckOut());
        }
    }

    // ---------- Extras (add-ons) and coupons ----------
    public record AddOnRequest(
            @NotBlank(message = "Name is required") @Size(max = 60, message = "Name is too long") String name,
            @Size(max = 200, message = "Description is too long") String description,
            @NotNull(message = "Price is required") @DecimalMin(value = "0", message = "Price cannot be negative")
            @Digits(integer = 7, fraction = 2, message = "Invalid price") BigDecimal price,
            boolean perNight,
            Boolean active) {}

    public record CouponRequest(
            @NotBlank(message = "Code is required") @Size(max = 30, message = "Code is too long")
            @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Code can contain letters, numbers, - and _ only") String code,
            @Size(max = 200, message = "Description is too long") String description,
            @NotNull(message = "Discount type is required") Coupon.DiscountType type,
            @NotNull(message = "Discount value is required") @DecimalMin(value = "1", message = "Discount must be at least 1") BigDecimal value,
            @DecimalMin(value = "0", message = "Minimum amount cannot be negative") BigDecimal minAmount,
            @DecimalMin(value = "1", message = "Maximum discount must be at least 1") BigDecimal maxDiscount,
            LocalDate validFrom,
            LocalDate validTo,
            @Min(value = 1, message = "Usage limit must be at least 1") Integer usageLimit,
            Boolean active) {}

    /** What customers see in the "Offers" strip. */
    public record PublicOffer(String code, String description, String type, BigDecimal value,
                              BigDecimal minAmount, BigDecimal maxDiscount, LocalDate validTo) {
        public static PublicOffer of(Coupon c) {
            return new PublicOffer(c.getCode(), c.getDescription(), c.getType().name(), c.getValue(),
                    c.getMinAmount(), c.getMaxDiscount(), c.getValidTo());
        }
    }

    // ---------- Staff and housekeeping ----------
    public record StaffRequest(
            @NotBlank(message = "Name is required") @Size(max = 100, message = "Name is too long") String fullName,
            @NotNull(message = "Role is required") Staff.Role role,
            @Pattern(regexp = PHONE_REGEX, message = PHONE_MSG) String phone,
            @Email(message = "Invalid email") @Size(max = 120, message = "Email is too long") String email,
            Boolean active) {}

    public record TaskCreateRequest(
            @NotNull(message = "Room is required") Long roomId,
            @NotNull(message = "Task type is required") HousekeepingTask.Type type,
            @Size(max = 300, message = "Notes are too long") String notes,
            Long staffId) {}

    /** Any field left out (null) stays unchanged. Send unassign=true to remove the assigned staff member. */
    public record TaskUpdateRequest(
            HousekeepingTask.Status status,
            Long staffId,
            Boolean unassign,
            @Size(max = 300, message = "Notes are too long") String notes) {}

    public record TaskView(Long id, Long roomId, String roomNumber, String roomType, String type, String status,
                           Long staffId, String staffName, String notes,
                           LocalDateTime createdAt, LocalDateTime completedAt) {
        public static TaskView of(HousekeepingTask t) {
            Staff s = t.getAssignedTo();
            return new TaskView(t.getId(), t.getRoom().getId(), t.getRoom().getRoomNumber(),
                    t.getRoom().getType().name(), t.getType().name(), t.getStatus().name(),
                    s == null ? null : s.getId(), s == null ? null : s.getFullName(),
                    t.getNotes(), t.getCreatedAt(), t.getCompletedAt());
        }
    }

    /** state: CLEAN, DIRTY (cleaning pending) or MAINTENANCE (repair pending). */
    public record RoomStatusView(Long roomId, String roomNumber, String roomType, boolean active,
                                 String state, int openTasks) {}

    // ---------- Food menu and room-service orders ----------
    public record MenuItemRequest(
            @NotBlank(message = "Name is required") @Size(max = 80, message = "Name is too long") String name,
            @Size(max = 200, message = "Description is too long") String description,
            @NotNull(message = "Category is required") MenuItem.Category category,
            @NotNull(message = "Price is required") @DecimalMin(value = "1", message = "Price must be positive")
            @Digits(integer = 6, fraction = 2, message = "Invalid price") BigDecimal price,
            boolean veg,
            @Size(max = 500, message = "Image link is too long") String imageUrl,
            Boolean available) {}

    public record OrderItemRequest(
            @NotNull(message = "Menu item is required") Long menuItemId,
            @Min(value = 1, message = "Quantity must be at least 1") @Max(value = 10, message = "You can order at most 10 of one item") int quantity) {}

    public record OrderRequest(
            @NotNull(message = "Booking is required") Long bookingId,
            @NotEmpty(message = "Add at least one item") @Size(max = 20, message = "Too many items in one order") List<@Valid OrderItemRequest> items,
            @Size(max = 200, message = "Notes are too long") String notes) {}

    public record OrderStatusRequest(@NotNull(message = "Status is required") FoodOrder.Status status) {}

    public record OrderView(Long id, Long bookingId, String roomNumber, String customerName,
                            List<OrderLine> items, BigDecimal totalAmount, String status, boolean paid,
                            String notes, LocalDateTime createdAt) {
        public static OrderView of(FoodOrder o) {
            Booking b = o.getBooking();
            return new OrderView(o.getId(), b.getId(), b.getRoom().getRoomNumber(), b.getUser().getFullName(),
                    List.copyOf(o.getItems()), o.getTotalAmount(), o.getStatus().name(), o.isPaid(),
                    o.getNotes(), o.getCreatedAt());
        }
    }

    // ---------- Admin ----------
    public record Stats(long totalRooms, long totalBookings, long confirmed, long checkedIn,
                        long cancelled, long occupiedToday, BigDecimal revenue, long customers) {}

    public record MonthlyRow(String month, long payments, BigDecimal revenue) {}
}
