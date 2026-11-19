package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.model.AddOn;
import com.hotel.model.Coupon;
import com.hotel.model.MenuItem;
import com.hotel.repository.AddOnRepository;
import com.hotel.repository.CouponRepository;
import com.hotel.repository.MenuItemRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Public (no login needed): current offers, bookable extras and the food menu. */
@RestController
@RequestMapping("/api")
public class OfferController {
    private final CouponRepository coupons;
    private final AddOnRepository addOns;
    private final MenuItemRepository menu;

    public OfferController(CouponRepository coupons, AddOnRepository addOns, MenuItemRepository menu) {
        this.coupons = coupons;
        this.addOns = addOns;
        this.menu = menu;
    }

    @GetMapping("/offers")
    public List<Dto.PublicOffer> offers() {
        LocalDate today = LocalDate.now();
        return coupons.findAllByOrderByIdDesc().stream()
                .filter(Coupon::isActive)
                .filter(c -> c.getValidFrom() == null || !today.isBefore(c.getValidFrom()))
                .filter(c -> c.getValidTo() == null || !today.isAfter(c.getValidTo()))
                .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit())
                .map(Dto.PublicOffer::of)
                .toList();
    }

    @GetMapping("/addons")
    public List<AddOn> addOns() {
        return addOns.findByActiveTrueOrderByNameAsc();
    }

    @GetMapping("/menu")
    public List<MenuItem> menu() {
        return menu.findByAvailableTrueOrderByCategoryAscNameAsc();
    }
}
