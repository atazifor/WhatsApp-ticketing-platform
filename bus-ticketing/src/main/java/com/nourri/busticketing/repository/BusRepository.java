package com.nourri.busticketing.repository;

import com.nourri.busticketing.model.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface BusRepository extends JpaRepository<Bus, UUID> {
    // Add custom query methods here if needed
}
