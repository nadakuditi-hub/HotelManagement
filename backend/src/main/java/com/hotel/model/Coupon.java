package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A discount code applied when booking. */
@Entity
@Table(name = "coupons")
public class Coupon {
    public enum DiscountType { PERCENT, FLAT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType type;

    /** Percent (e.g. 10) or flat rupee amount, depending on type. */
    @Column(name = "discount_value", nullable = false)
    private BigDecimal value;

    /** Minimum booking amount (room + extras) needed to use the coupon. */
    @Column(name = "min_amount")
    private BigDecimal minAmount;

    /** Upper limit of the discount for percentage coupons. */
    @Column(name = "max_discount")
    private BigDecimal maxDiscount;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(nullable = false)
    private boolean active = true;

    public Coupon() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DiscountType getType() { return type; }
    public void setType(DiscountType type) { this.type = type; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
