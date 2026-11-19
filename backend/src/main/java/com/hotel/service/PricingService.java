package com.hotel.service;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.AddOn;
import com.hotel.model.AddOnLine;
import com.hotel.model.Coupon;
import com.hotel.model.Room;
import com.hotel.repository.AddOnRepository;
import com.hotel.repository.CouponRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Works out the price of a stay: room + extras - coupon. All money is calculated here, never on the client. */
@Service
public class PricingService {
    /** The final price plus the coupon that was applied (so the caller can count its use). */
    public record Priced(Dto.Quote quote, Coupon coupon) {}

    private final AddOnRepository addOns;
    private final CouponRepository coupons;

    public PricingService(AddOnRepository addOns, CouponRepository coupons) {
        this.addOns = addOns;
        this.coupons = coupons;
    }

    /**
     * @param strictCoupon true when really booking (an invalid coupon is an error);
     *                     false for a price preview (an invalid coupon is reported in couponError).
     */
    public Priced price(Room room, LocalDate checkIn, LocalDate checkOut, List<Long> addOnIds,
                        String couponCode, boolean strictCoupon) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal roomAmount = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        List<AddOnLine> lines = new ArrayList<>();
        BigDecimal extras = BigDecimal.ZERO;
        if (addOnIds != null) {
            for (Long id : addOnIds.stream().filter(Objects::nonNull).distinct().toList()) {
                AddOn a = addOns.findById(id).filter(AddOn::isActive)
                        .orElseThrow(() -> ApiException.badRequest("One of the selected extras is not available"));
                BigDecimal amount = a.isPerNight() ? a.getPrice().multiply(BigDecimal.valueOf(nights)) : a.getPrice();
                lines.add(new AddOnLine(a.getName(), amount));
                extras = extras.add(amount);
            }
        }
        BigDecimal subtotal = roomAmount.add(extras);

        Coupon applied = null;
        String code = null;
        String couponError = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            try {
                applied = validate(couponCode.trim().toUpperCase(), subtotal);
                code = applied.getCode();
                discount = discountFor(applied, subtotal);
            } catch (ApiException e) {
                if (strictCoupon) throw e;
                couponError = e.getMessage();
                applied = null;
            }
        }

        BigDecimal total = subtotal.subtract(discount);
        return new Priced(new Dto.Quote(nights, roomAmount, lines, extras, discount, code, couponError, total), applied);
    }

    private Coupon validate(String code, BigDecimal subtotal) {
        Coupon c = coupons.findByCode(code).orElseThrow(() -> ApiException.badRequest("Invalid coupon code"));
        LocalDate today = LocalDate.now();
        if (!c.isActive()) throw ApiException.badRequest("This coupon is not active");
        if (c.getValidFrom() != null && today.isBefore(c.getValidFrom()))
            throw ApiException.badRequest("This coupon is not valid yet");
        if (c.getValidTo() != null && today.isAfter(c.getValidTo()))
            throw ApiException.badRequest("This coupon has expired");
        if (c.getUsageLimit() != null && c.getUsedCount() >= c.getUsageLimit())
            throw ApiException.badRequest("This coupon has reached its usage limit");
        if (c.getMinAmount() != null && subtotal.compareTo(c.getMinAmount()) < 0)
            throw ApiException.badRequest("Minimum booking amount for this coupon is Rs " + c.getMinAmount().stripTrailingZeros().toPlainString());
        return c;
    }

    private BigDecimal discountFor(Coupon c, BigDecimal subtotal) {
        BigDecimal d;
        if (c.getType() == Coupon.DiscountType.PERCENT) {
            d = subtotal.multiply(c.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (c.getMaxDiscount() != null && d.compareTo(c.getMaxDiscount()) > 0) d = c.getMaxDiscount();
        } else {
            d = c.getValue();
        }
        if (d.compareTo(subtotal) > 0) d = subtotal;
        return d.setScale(2, RoundingMode.HALF_UP);
    }
}
