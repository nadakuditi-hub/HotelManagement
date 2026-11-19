package com.hotel.repository;

import com.hotel.model.HousekeepingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Long> {
    List<HousekeepingTask> findAllByOrderByCreatedAtDesc();

    /** Unfinished tasks (OPEN or IN_PROGRESS) when called with Status.DONE. */
    List<HousekeepingTask> findByStatusNot(HousekeepingTask.Status status);

    boolean existsByRoomIdAndStatusNot(Long roomId, HousekeepingTask.Status status);
}
