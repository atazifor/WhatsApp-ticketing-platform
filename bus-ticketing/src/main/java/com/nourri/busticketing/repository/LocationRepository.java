package com.nourri.busticketing.repository;

import com.nourri.busticketing.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    // Add custom query methods here if needed
}
