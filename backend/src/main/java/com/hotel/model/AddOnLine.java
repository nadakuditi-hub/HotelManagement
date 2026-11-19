package com.hotel.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** One extra (breakfast, extra bed...) charged on a booking. Prices are copied at booking time. */
@Embeddable
public class AddOnLine {
    private String name;
    private BigDecimal amount;

    public AddOnLine() {}

    public AddOnLine(String name, BigDecimal amount) {
        this.name = name;
        this.amount = amount;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
