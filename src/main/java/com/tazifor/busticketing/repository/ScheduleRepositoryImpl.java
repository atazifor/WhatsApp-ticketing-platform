package com.tazifor.busticketing.repository;

import com.tazifor.busticketing.model.Schedule;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Repository
public class ScheduleRepositoryImpl implements ScheduleRepositoryCustom {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScheduleRepositoryImpl.class);

    @PersistenceContext
    private EntityManager em;


    @Override
    public List<Schedule> findSchedules(String origin, String destination, String travelDate, List<String> classes,
                                        List<String> agencies, List<String> departureTimes) {
        // Convert string times to LocalTime objects
        List<LocalTime> times = (departureTimes != null && !departureTimes.isEmpty())
            ? departureTimes.stream().map(LocalTime::parse).toList()
            : null;

        // Parse travelDate if provided
        LocalDate parsedDate = null;
        if (travelDate != null && !travelDate.isEmpty()) {
            try {
                parsedDate = LocalDate.parse(travelDate);
                logger.debug("Parsed travel date: {}", parsedDate);
            } catch (DateTimeParseException e) {
                logger.error("Failed to parse travel date: {}", travelDate, e);
            }
        }

        // Start building the JPQL query
        StringBuilder queryStr = new StringBuilder(
            "SELECT DISTINCT s FROM Schedule s " +
                "JOIN FETCH s.fromLocation fl " +
                "JOIN FETCH s.toLocation tl " +
                "JOIN FETCH s.agency a " +
                "WHERE 1=1 "
        );

        // Track which parameters will be added
        Map<String, Object> params = new HashMap<>();

        if (origin != null) {
            queryStr.append(" AND LOWER(fl.name) = LOWER(:origin)");
            params.put("origin", origin);
        }

        if (destination != null) {
            queryStr.append(" AND LOWER(tl.name) = LOWER(:destination)");
            params.put("destination", destination);
        }

        if (agencies != null && !agencies.isEmpty()) {
            queryStr.append(" AND a.name IN :agencies");
            params.put("agencies", agencies);
        }

        if (times != null && !times.isEmpty()) {
            queryStr.append(" AND s.departureTime IN :times");
            params.put("times", times);
        }

        // Add travel date filtering
        if (parsedDate != null) {
            queryStr.append(" AND s.travelDate = :travelDate");
            params.put("travelDate", parsedDate);
        }

        // Revised class filtering logic
        if (classes != null && !classes.isEmpty()) {
            queryStr.append(" AND EXISTS (SELECT 1 FROM s.scheduleClassPrices scp " +
                "JOIN scp.travelClass tc WHERE tc.name IN :classes)");
            params.put("classes", classes);
        }

        // Create the query
        Query query = em.createQuery(queryStr.toString(), Schedule.class);
        params.forEach(query::setParameter);

        logger.debug("Executing query: {}", queryStr.toString());
        logger.debug("With parameters: {}", params);

        // Execute query
        List<Schedule> results = query.getResultList();
        logger.debug("Found {} results for date {}", results.size(), parsedDate);

        // If results found, fetch class prices in a separate query
        if (!results.isEmpty() && (classes == null || !classes.isEmpty())) {
            List<UUID> scheduleIds = results.stream().map(Schedule::getId).toList();

            String fetchQuery = """
            SELECT DISTINCT s FROM Schedule s
            LEFT JOIN FETCH s.scheduleClassPrices scp
            LEFT JOIN FETCH scp.travelClass tc
            WHERE s.id IN :scheduleIds
            """;

            em.createQuery(fetchQuery, Schedule.class)
                .setParameter("scheduleIds", scheduleIds)
                .getResultList();
        }

        return results;
    }


    @Override
    public Optional<Schedule> findScheduleDetails(String agency, String from, String to,
                                                  String travelClass, String departureTimeStr, String tavelDate) {

            logger.debug("[findScheduleDetails] Looking for schedule details for agency {}, from {}, to {}, class {}, departure time {}, date {}",
                agency, from, to, travelClass, departureTimeStr, tavelDate);

        List<Schedule> schedules = this.findSchedules(from, to, tavelDate,
            Collections.singletonList(travelClass),
            Collections.singletonList(agency),
            Collections.singletonList(departureTimeStr)
        );
        if (!schedules.isEmpty()) {
            // Since we filtered by exact time/date/agency, there should be only one match
            return Optional.of(schedules.get(0));
        }

        return Optional.empty();
    }

    @Override
    public List<Schedule> findScheduleByDate(LocalDate date) {
        return em.createQuery("""
        SELECT s FROM Schedule s
        JOIN FETCH s.fromLocation
        JOIN FETCH s.toLocation
        WHERE s.travelDate = :date
        """, Schedule.class)
            .setParameter("date", date)
            .getResultList();
    }

}
