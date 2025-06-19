package com.nourri.busticketing.repository;

import com.nourri.busticketing.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    // In your BookingRepository
    @Query("""
          SELECT b
          FROM Booking b
          LEFT JOIN FETCH b.tickets t
          WHERE b.id = :id
        """)
    Optional<Booking> findByIdWithTickets(@Param("id") UUID id);

}
