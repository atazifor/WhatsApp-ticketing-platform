package com.tazifor.busticketing.repository;

import com.tazifor.busticketing.model.Schedule;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    // Add custom query methods here if needed
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Schedule s WHERE s.id = :id")
    Optional<Schedule> findByIdWithLock(@Param("id") UUID id);

    @Query("""
        SELECT s FROM Schedule s
        JOIN FETCH s.agency a
        JOIN FETCH s.fromLocation fl
        JOIN FETCH s.toLocation tl
        WHERE LOWER(a.name) = LOWER(:agency)
          AND LOWER(fl.name) = LOWER(:from)
          AND LOWER(tl.name) = LOWER(:to)
          AND s.travelDate = :date
          AND s.departureTime = :time
        """)
    Schedule findByAgencyAndLocationsAndDateTime(
        @Param("agency") String agency,
        @Param("from") String from,
        @Param("to") String to,
        @Param("date") LocalDate date,
        @Param("time") LocalTime time
    );

    @Query("""
         SELECT s
         FROM Schedule s
         JOIN FETCH s.scheduleClassPrices scp
         JOIN FETCH scp.travelClass tc
         WHERE s.id = :id
        """)
    Optional<Schedule> findByIdWithPrices(@Param("id") UUID id);

}
