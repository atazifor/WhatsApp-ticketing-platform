package com.tazifor.busticketing.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazifor.busticketing.config.AgencyConfig;
import com.tazifor.busticketing.model.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class ScheduleDetailsLoader {
    private final List<ScheduleDetails> scheduleDetails = new ArrayList<>();
    @Getter
    private final Map<String, AgencyConfig> agencyConfigs = new HashMap<>();
    @Getter
    private final Map<String, List<AgencyContact>> agencyContacts = new HashMap<>();

    @PostConstruct
    public void load() {
        try {
            InputStream is = getClass().getResourceAsStream("/data/seed_data.json");
            loadFromStream(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load schedule details from JSON", e);
        }
    }

    public void loadFromStream(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(is);

        for (JsonNode agency : root) {
            String agencyName = agency.get("name").asText();

            //load agency contact
            List<AgencyContact> contacts = new ArrayList<>();
            for (JsonNode contact : agency.get("contacts")) {
                contacts.add(new AgencyContact(
                    contact.get("city").asText(),
                    contact.get("address").asText(),
                    contact.get("phone").asText()
                ));
            }
            agencyContacts.put(agencyName, contacts);

            // load agency config
            JsonNode configNode = agency.get("config");
            agencyConfigs.put(agencyName, new AgencyConfig(
                agencyName,
                configNode.get("maxTicketsPerBooking").asInt()
            ));

            // schedules
            for (JsonNode sched : agency.get("schedules")) {
                String from = sched.get("from").asText();
                String to = sched.get("to").asText();
                String time = sched.get("departureTime").asText();
                boolean openSeating = sched.get("openSeating").asBoolean();
                int totalSeats = sched.get("totalSeats").asInt();

                for (JsonNode cls : sched.get("seatClasses")) {
                    String className = cls.get("className").asText();
                    int price = cls.get("price").asInt();
                    int seatCount = cls.get("seatCount").asInt();
                    int sold = cls.get("sold").asInt();

                    AgencySchedule schedule = new AgencySchedule(
                        agencyName, from, to, className, price, time
                    );

                    Map<String, List<Seat>> seatMap = new HashMap<>();
                    Map<String, Integer> soldMap = new HashMap<>();

                    if (openSeating) {
                        soldMap.put(className, sold);
                    } else {
                        List<Seat> seats = new ArrayList<>();
                        for (JsonNode sn : cls.get("seats")) {
                            seats.add(new Seat(
                                sn.get("seatNumber").asText(),
                                sn.get("isSold").asBoolean()
                            ));
                        }
                        seatMap.put(className, seats);
                    }

                    SeatingInfo seatingInfo = new SeatingInfo(openSeating, totalSeats, seatMap, soldMap);
                    scheduleDetails.add(new ScheduleDetails(schedule, seatingInfo));
                }
            }
        }
    }

    public List<ScheduleDetails> getAllSchedules() {
        return scheduleDetails;
    }

    public AgencyConfig getConfigForAgency(String agencyName) {
        return agencyConfigs.getOrDefault(agencyName, new AgencyConfig(agencyName, 4));
    }

    public Optional<AgencyContact> findContactForAgency(String agency, String city) {
        return agencyContacts.getOrDefault(agency, List.of()).stream()
            .filter(c -> c.city().equalsIgnoreCase(city))
            .findFirst();
    }

}


