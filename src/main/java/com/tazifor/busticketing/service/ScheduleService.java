package com.tazifor.busticketing.service;

import com.tazifor.busticketing.model.Schedule;
import com.tazifor.busticketing.model.TravelClass;
import com.tazifor.busticketing.repository.ScheduleRepository;
import com.tazifor.busticketing.repository.ScheduleRepositoryCustom;
import com.tazifor.busticketing.repository.SeatRepository;
import com.tazifor.busticketing.repository.TravelClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScheduleService.class);

    private final ScheduleRepositoryCustom scheduleRepositoryCustom;
    private final ScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final TravelClassRepository travelClassRepository;

    /**
     * Finds all schedules matching the given filters.
     */
    @Transactional(readOnly = true)
    public List<Schedule> findSchedules(String origin, String destination, String travelDate, List<String> classes,
                                        List<String> agencies, List<String> departureTimes) {
        return scheduleRepositoryCustom.findSchedules(origin, destination, travelDate, classes,
            agencies, departureTimes);
    }

    /**
     * Finds one schedule using strict match (used before confirming a booking).
     */
    public Optional<Schedule> findScheduleDetails(String agency, String from, String to,
                                                  String travelClass, String departureTime, String travelDate) {
        return scheduleRepositoryCustom.findScheduleDetails(agency, from, to, travelClass, departureTime, travelDate);
    }

    // (Optional) future method to support real date filtering
    public List<Schedule> findByDate(LocalDate date) {
        return scheduleRepositoryCustom.findScheduleByDate(date);
    }


    public int getUnsoldSeats(String agency, String from, String to, String travelClass, String date, String time) {
        try {
            logger.debug("Starting getUnsoldSeats with params - agency: {}, from: {}, to: {}, class: {}, date: {}, time: {}",
                agency, from, to, travelClass, date, time);
            logger.debug("Parsed local date {} Local time {}", LocalDate.parse(date), LocalTime.parse(time));
            // First get the exact schedule
            logger.debug("Attempting to find schedule...");
            Schedule schedule = scheduleRepository.findByAgencyAndLocationsAndDateTime(
                agency, from, to, LocalDate.parse(date), LocalTime.parse(time));

            if (schedule == null) {
                logger.warn("No schedule found for given parameters");
                return 0;
            }
            logger.debug("Found schedule with ID: {}", schedule.getId());

            // Then get the exact travel class
            logger.debug("Looking for travel class: {} for agency ID: {}", travelClass, schedule.getAgency().getId());
            TravelClass tc = travelClassRepository
                .findByNameAndAgency(travelClass, schedule.getAgency().getId())
                .orElse(null);

            if (tc == null) {
                logger.warn("No travel class found for name: {} and agency ID: {}",
                    travelClass, schedule.getAgency().getId());
                return 0;
            }
            logger.debug("Found travel class with ID: {}", tc.getId());

            // Now count using IDs only
            logger.debug("Counting unsold seats for schedule ID: {} and travel class ID: {}",
                schedule.getId(), tc.getId());
            int unsoldSeats = seatRepository.countByScheduleIdAndTravelClassIdAndIsSoldFalse(
                schedule.getId(),
                tc.getId()
            );
            logger.debug("Found {} unsold seats", unsoldSeats);

            return unsoldSeats;

        } catch (DateTimeParseException e) {
            logger.error("Failed to parse date/time. Date: {}, Time: {}. Error: {}",
                date, time, e.getMessage());
            return 0;
        } catch (Exception e) {
            logger.error("Unexpected error in getUnsoldSeats: ", e);
            return 0;
        }
    }
}
