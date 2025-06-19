package com.nourri.busticketing.repository;

import com.nourri.busticketing.model.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgencyRepository extends JpaRepository<Agency, UUID> {
    // Add custom query methods here if needed
    Optional<Agency> findByNameIgnoreCase(String name);

}
