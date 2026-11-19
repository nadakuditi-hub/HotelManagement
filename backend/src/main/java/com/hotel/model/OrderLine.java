package com.hotel.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** One dish in a food order. Name and price are copied at order time. */
@Embeddable
public class OrderLine {
    private String name;
    private BigDecimal price;
    private int quantity;

    public OrderLine() {}

    public OrderLine(String name, BigDecimal price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
