package com.hotel.repository;

import com.hotel.model.AddOn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddOnRepository extends JpaRepository<AddOn, Long> {
    List<AddOn> findByActiveTrueOrderByNameAsc();

    List<AddOn> findAllByOrderByNameAsc();
}
