package com.hotel.controller;

import com.hotel.dto.Dto;
import com.hotel.exception.ApiException;
import com.hotel.model.AddOn;
import com.hotel.model.Coupon;
import com.hotel.model.HousekeepingTask;
import com.hotel.model.Room;
import com.hotel.model.Staff;
import com.hotel.repository.AddOnRepository;
import com.hotel.repository.CouponRepository;
import com.hotel.repository.HousekeepingTaskRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.StaffRepository;
import com.hotel.util.Text;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Admin: staff, housekeeping, extras (add-ons) and coupons. (/api/admin/** needs the ADMIN role.) */
@RestController
@RequestMapping("/api/admin")
public class AdminOpsController {
    private final StaffRepository staff;
    private final HousekeepingTaskRepository tasks;
    private final RoomRepository rooms;
    private final AddOnRepository addOns;
    private final CouponRepository coupons;

    public AdminOpsController(StaffRepository staff, HousekeepingTaskRepository tasks, RoomRepository rooms,
                              AddOnRepository addOns, CouponRepository coupons) {
        this.staff = staff;
        this.tasks = tasks;
        this.rooms = rooms;
        this.addOns = addOns;
        this.coupons = coupons;
    }

    // ================= Staff =================
    @GetMapping("/staff")
    public List<Staff> listStaff() {
        return staff.findAllByOrderByFullNameAsc();
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public Staff addStaff(@Valid @RequestBody Dto.StaffRequest req) {
        Staff s = new Staff();
        applyStaff(s, req);
        return staff.save(s);
    }

    @PutMapping("/staff/{id}")
    public Staff updateStaff(@PathVariable Long id, @Valid @RequestBody Dto.StaffRequest req) {
        Staff s = staff.findById(id).orElseThrow(() -> ApiException.notFound("Staff member not found"));
        applyStaff(s, req);
        return staff.save(s);
    }

    private void applyStaff(Staff s, Dto.StaffRequest req) {
        s.setFullName(req.fullName().trim());
        s.setRole(req.role());
        s.setPhone(Text.blankToNull(req.phone()));
        s.setEmail(Text.blankToNull(req.email()));
        if (req.active() != null) s.setActive(req.active());
    }

    // ================= Housekeeping =================
    /** Every room with its current state: CLEAN, DIRTY (cleaning pending) or MAINTENANCE. */
    @GetMapping("/housekeeping/rooms")
    public List<Dto.RoomStatusView> roomStatuses() {
        Map<Long, List<HousekeepingTask>> open = tasks.findByStatusNot(HousekeepingTask.Status.DONE).stream()
                .collect(Collectors.groupingBy(t -> t.getRoom().getId()));
        return rooms.findAll().stream()
                .sorted(Comparator.comparing(Room::getRoomNumber))
                .map(r -> {
                    List<HousekeepingTask> ts = open.getOrDefault(r.getId(), List.of());
                    String state = ts.stream().anyMatch(t -> t.getType() == HousekeepingTask.Type.MAINTENANCE)
                            ? "MAINTENANCE" : ts.isEmpty() ? "CLEAN" : "DIRTY";
                    return new Dto.RoomStatusView(r.getId(), r.getRoomNumber(), r.getType().name(), r.isActive(), state, ts.size());
                })
                .toList();
    }

    @GetMapping("/housekeeping/tasks")
    public List<Dto.TaskView> listTasks() {
        return tasks.findAllByOrderByCreatedAtDesc().stream().map(Dto.TaskView::of).toList();
    }

    @PostMapping("/housekeeping/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.TaskView createTask(@Valid @RequestBody Dto.TaskCreateRequest req) {
        Room room = rooms.findById(req.roomId()).orElseThrow(() -> ApiException.notFound("Room not found"));
        boolean duplicate = tasks.findByStatusNot(HousekeepingTask.Status.DONE).stream()
                .anyMatch(t -> t.getRoom().getId().equals(room.getId()) && t.getType() == req.type());
        if (duplicate)
            throw ApiException.conflict("Room " + room.getRoomNumber() + " already has a pending " + req.type().name().toLowerCase() + " task");
        HousekeepingTask t = new HousekeepingTask(room, req.type(), Text.blankToNull(req.notes()));
        if (req.staffId() != null) t.setAssignedTo(activeStaff(req.staffId()));
        return Dto.TaskView.of(tasks.save(t));
    }

    @PutMapping("/housekeeping/tasks/{id}")
    public Dto.TaskView updateTask(@PathVariable Long id, @Valid @RequestBody Dto.TaskUpdateRequest req) {
        HousekeepingTask t = tasks.findById(id).orElseThrow(() -> ApiException.notFound("Task not found"));
        if (req.status() != null) {
            t.setStatus(req.status());
            t.setCompletedAt(req.status() == HousekeepingTask.Status.DONE ? LocalDateTime.now() : null);
        }
        if (Boolean.TRUE.equals(req.unassign())) t.setAssignedTo(null);
        else if (req.staffId() != null) t.setAssignedTo(activeStaff(req.staffId()));
        if (req.notes() != null) t.setNotes(Text.blankToNull(req.notes()));
        return Dto.TaskView.of(tasks.save(t));
    }

    private Staff activeStaff(Long id) {
        return staff.findById(id).filter(Staff::isActive)
                .orElseThrow(() -> ApiException.badRequest("Staff member not found or not active"));
    }

    // ================= Extras (add-ons) =================
    @GetMapping("/addons")
    public List<AddOn> listAddOns() {
        return addOns.findAllByOrderByNameAsc();
    }

    @PostMapping("/addons")
    @ResponseStatus(HttpStatus.CREATED)
    public AddOn addAddOn(@Valid @RequestBody Dto.AddOnRequest req) {
        AddOn a = new AddOn();
        applyAddOn(a, req);
        return addOns.save(a);
    }

    @PutMapping("/addons/{id}")
    public AddOn updateAddOn(@PathVariable Long id, @Valid @RequestBody Dto.AddOnRequest req) {
        AddOn a = addOns.findById(id).orElseThrow(() -> ApiException.notFound("Extra not found"));
        applyAddOn(a, req);
        return addOns.save(a);
    }

    private void applyAddOn(AddOn a, Dto.AddOnRequest req) {
        a.setName(req.name().trim());
        a.setDescription(Text.blankToNull(req.description()));
        a.setPrice(req.price());
        a.setPerNight(req.perNight());
        if (req.active() != null) a.setActive(req.active());
    }

    // ================= Coupons =================
    @GetMapping("/coupons")
    public List<Coupon> listCoupons() {
        return coupons.findAllByOrderByIdDesc();
    }

    @PostMapping("/coupons")
    @ResponseStatus(HttpStatus.CREATED)
    public Coupon addCoupon(@Valid @RequestBody Dto.CouponRequest req) {
        String code = req.code().trim().toUpperCase();
        if (coupons.existsByCode(code)) throw ApiException.conflict("This coupon code already exists");
        Coupon c = new Coupon();
        applyCoupon(c, req);
        return coupons.save(c);
    }

    @PutMapping("/coupons/{id}")
    public Coupon updateCoupon(@PathVariable Long id, @Valid @RequestBody Dto.CouponRequest req) {
        Coupon c = coupons.findById(id).orElseThrow(() -> ApiException.notFound("Coupon not found"));
        String code = req.code().trim().toUpperCase();
        if (!c.getCode().equals(code) && coupons.existsByCode(code))
            throw ApiException.conflict("This coupon code already exists");
        applyCoupon(c, req);
        return coupons.save(c);
    }

    private void applyCoupon(Coupon c, Dto.CouponRequest req) {
        if (req.type() == Coupon.DiscountType.PERCENT && req.value().compareTo(BigDecimal.valueOf(100)) > 0)
            throw ApiException.badRequest("A percentage discount cannot be more than 100");
        if (req.validFrom() != null && req.validTo() != null && req.validTo().isBefore(req.validFrom()))
            throw ApiException.badRequest("The end date must not be before the start date");
        c.setCode(req.code().trim().toUpperCase());
        c.setDescription(Text.blankToNull(req.description()));
        c.setType(req.type());
        c.setValue(req.value());
        c.setMinAmount(req.minAmount());
        c.setMaxDiscount(req.type() == Coupon.DiscountType.PERCENT ? req.maxDiscount() : null);
        c.setValidFrom(req.validFrom());
        c.setValidTo(req.validTo());
        c.setUsageLimit(req.usageLimit());
        if (req.active() != null) c.setActive(req.active());
    }
}
