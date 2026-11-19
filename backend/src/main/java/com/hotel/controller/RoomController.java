package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.Room;
import com.hotel.repository.ReviewRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Public room browsing. Room management is in AdminController. */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomRepository rooms;
    private final ReviewRepository reviews;

    public RoomController(RoomRepository rooms, ReviewRepository reviews) {
        this.rooms = rooms;
        this.reviews = reviews;
    }

    /**
     * GET /api/rooms?checkIn=2026-11-01&checkOut=2026-11-03&type=DELUXE&guests=2
     *               &minPrice=1000&maxPrice=5000&sort=price_asc|price_desc|rating
     * Dates are optional; with dates only available rooms are returned.
     */
    @GetMapping
    public List<Dto.RoomView> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Room.Type type,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "price_asc") String sort) {

        List<Room> result;
        if (checkIn != null && checkOut != null) {
            if (!checkOut.isAfter(checkIn)) throw ApiException.badRequest("Check-out must be after check-in");
            result = rooms.findAvailable(checkIn, checkOut, BookingService.ACTIVE);
        } else {
            result = rooms.findByActiveTrue();
        }

        Map<Long, double[]> ratings = ratingMap();
        List<Dto.RoomView> views = result.stream()
                .filter(r -> type == null || r.getType() == type)
                .filter(r -> guests == null || r.getCapacity() >= guests)
                .filter(r -> minPrice == null || r.getPricePerNight().compareTo(minPrice) >= 0)
                .filter(r -> maxPrice == null || r.getPricePerNight().compareTo(maxPrice) <= 0)
                .map(r -> toView(r, ratings))
                .toList();

        Comparator<Dto.RoomView> byPrice = Comparator.comparing(Dto.RoomView::pricePerNight);
        Comparator<Dto.RoomView> cmp = switch (sort) {
            case "price_desc" -> byPrice.reversed();
            case "rating" -> Comparator
                    .comparing((Dto.RoomView v) -> v.avgRating() == null ? 0.0 : v.avgRating()).reversed()
                    .thenComparing(Comparator.comparingLong(Dto.RoomView::reviewCount).reversed())
                    .thenComparing(byPrice);
            default -> byPrice;
        };
        return views.stream().sorted(cmp).toList();
    }

    @GetMapping("/{id}")
    public Dto.RoomView get(@PathVariable Long id) {
        Room r = rooms.findById(id).orElseThrow(() -> ApiException.notFound("Room not found"));
        return toView(r, ratingMap());
    }

    @GetMapping("/{id}/reviews")
    public List<Dto.ReviewView> reviewsFor(@PathVariable Long id) {
        if (!rooms.existsById(id)) throw ApiException.notFound("Room not found");
        return reviews.findByRoomIdOrderByCreatedAtDesc(id).stream().map(Dto.ReviewView::of).toList();
    }

    // roomId -> [average rating, review count]
    private Map<Long, double[]> ratingMap() {
        Map<Long, double[]> map = new HashMap<>();
        for (Object[] row : reviews.ratingSummary()) {
            map.put((Long) row[0], new double[]{((Number) row[1]).doubleValue(), ((Number) row[2]).doubleValue()});
        }
        return map;
    }

    private Dto.RoomView toView(Room r, Map<Long, double[]> ratings) {
        double[] s = ratings.get(r.getId());
        return Dto.RoomView.of(r, s == null ? null : s[0], s == null ? 0 : (long) s[1]);
    }
}
