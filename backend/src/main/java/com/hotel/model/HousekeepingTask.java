package com.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A cleaning or maintenance job for a room. A room is "ready" only when it has no unfinished task.
 * A CLEANING task is created automatically when a guest checks out.
 */
@Entity
@Table(name = "housekeeping_tasks")
public class HousekeepingTask {
    public enum Type { CLEANING, MAINTENANCE }
    public enum Status { OPEN, IN_PROGRESS, DONE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "room_id")
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id")
    private Staff assignedTo;

    @Column(length = 300)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public HousekeepingTask() {}

    public HousekeepingTask(Room room, Type type, String notes) {
        this.room = room;
        this.type = type;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Staff getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Staff assignedTo) { this.assignedTo = assignedTo; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
