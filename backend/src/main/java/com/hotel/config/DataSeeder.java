package com.hotel.config;

import com.hotel.model.AddOn;
import com.hotel.model.Coupon;
import com.hotel.model.MenuItem;
import com.hotel.model.Room;
import com.hotel.model.Staff;
import com.hotel.model.User;
import com.hotel.repository.AddOnRepository;
import com.hotel.repository.CouponRepository;
import com.hotel.repository.MenuItemRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.StaffRepository;
import com.hotel.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Creates sample data on first start: admin account, rooms, extras, coupons, staff and a food menu.
 * Each part is only created while its table is empty, so your own changes are never overwritten.
 */
@Component
public class DataSeeder implements CommandLineRunner {
    /**
     * Sample room photos (Unsplash). If a link ever stops working, the website shows a drawn
     * placeholder instead, and the admin can upload a photo or paste a new link for the room.
     */
    private static final Map<Room.Type, String> PHOTOS = Map.of(
            Room.Type.SINGLE, "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?w=800&q=70",
            Room.Type.DOUBLE, "https://images.unsplash.com/photo-1618773928121-c32242e63f39?w=800&q=70",
            Room.Type.DELUXE, "https://images.unsplash.com/photo-1590490360182-c33d57733427?w=800&q=70",
            Room.Type.SUITE, "https://images.unsplash.com/photo-1566665797739-1674de7a421a?w=800&q=70");

    private final UserRepository users;
    private final RoomRepository rooms;
    private final PasswordEncoder encoder;
    private final AddOnRepository addOns;
    private final CouponRepository coupons;
    private final StaffRepository staff;
    private final MenuItemRepository menu;

    public DataSeeder(UserRepository users, RoomRepository rooms, PasswordEncoder encoder,
                      AddOnRepository addOns, CouponRepository coupons, StaffRepository staff,
                      MenuItemRepository menu) {
        this.users = users;
        this.rooms = rooms;
        this.encoder = encoder;
        this.addOns = addOns;
        this.coupons = coupons;
        this.staff = staff;
        this.menu = menu;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedRooms();
        seedAddOns();
        seedCoupons();
        seedStaff();
        seedMenu();
    }

    private void seedAdmin() {
        if (!users.existsByEmail("admin@hotel.com")) {
            User admin = new User();
            admin.setFullName("Hotel Admin");
            admin.setEmail("admin@hotel.com");
            admin.setPassword(encoder.encode("admin123"));
            admin.setRole(User.Role.ADMIN);
            users.save(admin);
        }
    }

    private void seedRooms() {
        if (rooms.count() == 0) {
            rooms.save(new Room("101", Room.Type.SINGLE, new BigDecimal("1500"), 1, "Cozy single room with city view"));
            rooms.save(new Room("102", Room.Type.SINGLE, new BigDecimal("1500"), 1, "Single room with work desk"));
            rooms.save(new Room("201", Room.Type.DOUBLE, new BigDecimal("2500"), 2, "Double bed, balcony, free breakfast"));
            rooms.save(new Room("202", Room.Type.DOUBLE, new BigDecimal("2500"), 2, "Twin beds, garden view"));
            rooms.save(new Room("301", Room.Type.DELUXE, new BigDecimal("4000"), 3, "Spacious deluxe room with bathtub"));
            rooms.save(new Room("302", Room.Type.DELUXE, new BigDecimal("4200"), 3, "Deluxe room, king bed, lounge area"));
            rooms.save(new Room("401", Room.Type.SUITE, new BigDecimal("7500"), 4, "Luxury suite with living room and jacuzzi"));
        }
        // Rooms created before photos existed get a default photo for their type.
        for (Room r : rooms.findAll()) {
            if (r.getImageUrl() == null || r.getImageUrl().isBlank()) {
                r.setImageUrl(PHOTOS.get(r.getType()));
                rooms.save(r);
            }
        }
    }

    private void seedAddOns() {
        if (addOns.count() > 0) return;
        addOns.save(new AddOn("Breakfast buffet", "Daily breakfast for the room", new BigDecimal("350"), true));
        addOns.save(new AddOn("Extra bed", "A comfortable extra bed in the room", new BigDecimal("800"), true));
        addOns.save(new AddOn("Airport pickup", "Pickup from the airport by car", new BigDecimal("1200"), false));
        addOns.save(new AddOn("Late check-out", "Keep the room until 4 PM", new BigDecimal("600"), false));
        addOns.save(new AddOn("Spa session", "60-minute relaxation massage", new BigDecimal("1500"), false));
    }

    private void seedCoupons() {
        if (coupons.count() > 0) return;
        LocalDate today = LocalDate.now();
        coupons.save(coupon("WELCOME10", "10% off your stay (up to Rs 1000)", Coupon.DiscountType.PERCENT,
                "10", "1500", "1000", today.plusYears(1)));
        coupons.save(coupon("FLAT500", "Flat Rs 500 off on stays above Rs 3000", Coupon.DiscountType.FLAT,
                "500", "3000", null, today.plusYears(1)));
        coupons.save(coupon("FESTIVE20", "Festive offer: 20% off (up to Rs 2500)", Coupon.DiscountType.PERCENT,
                "20", "5000", "2500", today.plusDays(60)));
    }

    private Coupon coupon(String code, String description, Coupon.DiscountType type, String value,
                          String min, String max, LocalDate validTo) {
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDescription(description);
        c.setType(type);
        c.setValue(new BigDecimal(value));
        c.setMinAmount(min == null ? null : new BigDecimal(min));
        c.setMaxDiscount(max == null ? null : new BigDecimal(max));
        c.setValidFrom(LocalDate.now());
        c.setValidTo(validTo);
        return c;
    }

    private void seedStaff() {
        if (staff.count() > 0) return;
        staff.save(new Staff("Ravi Kumar", Staff.Role.MANAGER, "9876500001", "ravi@grandstay.example"));
        staff.save(new Staff("Anita Sharma", Staff.Role.RECEPTIONIST, "9876500002", "anita@grandstay.example"));
        staff.save(new Staff("Lakshmi Devi", Staff.Role.HOUSEKEEPING, "9876500003", null));
        staff.save(new Staff("Suresh Babu", Staff.Role.HOUSEKEEPING, "9876500004", null));
        staff.save(new Staff("Imran Khan", Staff.Role.MAINTENANCE, "9876500005", null));
        staff.save(new Staff("Chef Prakash", Staff.Role.CHEF, "9876500006", null));
    }

    private void seedMenu() {
        if (menu.count() > 0) return;
        MenuItem.Category b = MenuItem.Category.BREAKFAST, s = MenuItem.Category.STARTER,
                m = MenuItem.Category.MAIN_COURSE, d = MenuItem.Category.DESSERT, v = MenuItem.Category.BEVERAGE;

        item("Masala Dosa", "Crisp dosa with potato masala, sambar and chutneys", b, "120", true);
        item("Idli Vada Combo", "Two idlis and a vada with sambar", b, "90", true);
        item("Aloo Paratha", "Stuffed paratha served with curd and pickle", b, "110", true);
        item("Poha", "Flattened rice with peanuts and lemon", b, "80", true);
        item("Egg Omelette with Toast", "Three-egg masala omelette with buttered toast", b, "100", false);
        item("Fresh Fruit Bowl", "Seasonal cut fruits", b, "130", true);

        item("Paneer Tikka", "Char-grilled cottage cheese with mint chutney", s, "280", true);
        item("Veg Spring Rolls", "Crispy rolls with sweet chilli sauce", s, "200", true);
        item("Chicken 65", "Spicy fried chicken with curry leaves", s, "260", false);
        item("Fish Fingers", "Crumb-fried fish with tartar sauce", s, "320", false);

        item("Paneer Butter Masala", "Paneer in rich tomato-butter gravy", m, "320", true);
        item("Dal Tadka", "Yellow lentils tempered with garlic and cumin", m, "220", true);
        item("Veg Kolhapuri", "Spicy mixed vegetable curry", m, "280", true);
        item("Butter Chicken", "Tandoori chicken in creamy tomato gravy", m, "380", false);
        item("Mutton Rogan Josh", "Slow-cooked mutton in Kashmiri spices", m, "450", false);
        item("Hyderabadi Chicken Biryani", "Dum biryani with raita and salan", m, "350", false);
        item("Veg Dum Biryani", "Fragrant basmati rice with vegetables and raita", m, "280", true);
        item("Butter Naan (2 pcs)", "Soft tandoori bread brushed with butter", m, "80", true);
        item("Jeera Rice", "Basmati rice tempered with cumin", m, "150", true);

        item("Gulab Jamun", "Two warm milk dumplings in sugar syrup", d, "120", true);
        item("Rasmalai", "Soft cottage cheese discs in saffron milk", d, "140", true);
        item("Ice Cream Sundae", "Vanilla ice cream with chocolate sauce and nuts", d, "160", true);
        item("Chocolate Brownie", "Warm brownie with ice cream", d, "180", true);

        item("Masala Chai", "Indian spiced milk tea", v, "50", true);
        item("Filter Coffee", "South Indian filter coffee", v, "60", true);
        item("Fresh Lime Soda", "Sweet, salted or mixed", v, "80", true);
        item("Mango Lassi", "Thick yogurt drink with mango", v, "110", true);
        item("Cold Coffee", "Chilled coffee with ice cream", v, "120", true);
    }

    private void item(String name, String description, MenuItem.Category category, String price, boolean veg) {
        menu.save(new MenuItem(name, description, category, new BigDecimal(price), veg));
    }
}
