package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.MenuItem;
import com.hotel.repository.MenuItemRepository;
import com.hotel.service.FileStorageService;
import com.hotel.service.OrderService;
import com.hotel.util.Text;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Admin: manage the food menu and room-service orders. (/api/admin/** needs the ADMIN role.) */
@RestController
@RequestMapping("/api/admin")
public class AdminFoodController {
    private final MenuItemRepository menu;
    private final OrderService orders;
    private final FileStorageService storage;

    public AdminFoodController(MenuItemRepository menu, OrderService orders, FileStorageService storage) {
        this.menu = menu;
        this.orders = orders;
        this.storage = storage;
    }

    // ---------- Menu ----------
    @GetMapping("/menu")
    public List<MenuItem> allMenuItems() {
        return menu.findAllByOrderByCategoryAscNameAsc();
    }

    @PostMapping("/menu")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItem addMenuItem(@Valid @RequestBody Dto.MenuItemRequest req) {
        MenuItem m = new MenuItem();
        apply(m, req);
        return menu.save(m);
    }

    @PutMapping("/menu/{id}")
    public MenuItem updateMenuItem(@PathVariable Long id, @Valid @RequestBody Dto.MenuItemRequest req) {
        MenuItem m = menu.findById(id).orElseThrow(() -> ApiException.notFound("Menu item not found"));
        apply(m, req);
        return menu.save(m);
    }

    /** Upload (or replace) a dish photo from your computer. Form field name: "file". JPG/PNG/WEBP, max 5 MB. */
    @PostMapping("/menu/{id}/image")
    public MenuItem uploadMenuImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        MenuItem m = menu.findById(id).orElseThrow(() -> ApiException.notFound("Menu item not found"));
        String old = m.getImageUrl();
        m.setImageUrl(storage.saveImage(file));
        MenuItem saved = menu.save(m);
        storage.delete(old);
        return saved;
    }

    /** Remove the dish photo; the dish then shows its name on a plain tile. */
    @DeleteMapping("/menu/{id}/image")
    public MenuItem removeMenuImage(@PathVariable Long id) {
        MenuItem m = menu.findById(id).orElseThrow(() -> ApiException.notFound("Menu item not found"));
        String old = m.getImageUrl();
        m.setImageUrl(null);
        MenuItem saved = menu.save(m);
        storage.delete(old);
        return saved;
    }

    private void apply(MenuItem m, Dto.MenuItemRequest req) {
        m.setName(req.name().trim());
        m.setDescription(Text.blankToNull(req.description()));
        m.setCategory(req.category());
        m.setPrice(req.price());
        m.setVeg(req.veg());
        m.setImageUrl(Text.safeImageUrl(req.imageUrl()));
        if (req.available() != null) m.setAvailable(req.available());
    }

    // ---------- Orders ----------
    @GetMapping("/orders")
    public List<Dto.OrderView> allOrders() {
        return orders.all();
    }

    @PutMapping("/orders/{id}/status")
    public Dto.OrderView updateOrderStatus(@PathVariable Long id, @Valid @RequestBody Dto.OrderStatusRequest req) {
        return orders.updateStatus(id, req.status());
    }

    @PutMapping("/orders/{id}/paid")
    public Dto.OrderView markOrderPaid(@PathVariable Long id) {
        return orders.markPaid(id);
    }
}
