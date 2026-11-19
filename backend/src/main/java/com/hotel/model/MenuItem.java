package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** A dish or drink on the hotel's food menu. */
@Entity
@Table(name = "menu_items")
public class MenuItem {
    public enum Category { BREAKFAST, STARTER, MAIN_COURSE, DESSERT, BEVERAGE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private BigDecimal price;

    /** true = vegetarian, false = non-vegetarian. */
    @Column(nullable = false)
    private boolean veg = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private boolean available = true;

    public MenuItem() {}

    public MenuItem(String name, String description, Category category, BigDecimal price, boolean veg) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.veg = veg;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isVeg() { return veg; }
    public void setVeg(boolean veg) { this.veg = veg; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
