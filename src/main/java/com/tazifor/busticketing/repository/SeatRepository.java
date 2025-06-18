package com.tazifor.busticketing.repository;

import com.tazifor.busticketing.model.Schedule;
import com.tazifor.busticketing.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {
    // Add custom query methods here if needed
    List<Seat> findBySchedule(Schedule schedule);

    int countByScheduleIdAndTravelClassIdAndIsSoldFalse(UUID scheduleId, UUID travelClassId);

    @Query("""
            SELECT s FROM Seat s
            WHERE s.schedule = :schedule AND s.travelClass.name = :travelClass AND s.isSold = false
        """)
    List<Seat> findAvailableSeatsByScheduleAndClass(@Param("schedule") Schedule schedule, @Param("travelClass") String travelClass);

}
