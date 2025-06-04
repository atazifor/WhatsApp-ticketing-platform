package com.tazifor.busticketing.service;

import com.tazifor.busticketing.dto.FinalScreenResponsePayload;
import com.tazifor.busticketing.dto.FlowDataExchangePayload;
import com.tazifor.busticketing.dto.FlowResponsePayload;
import com.tazifor.busticketing.dto.NextScreenResponsePayload;
import com.tazifor.busticketing.model.BookingState;
import com.tazifor.busticketing.util.BeanUtil;

import java.util.*;
import java.util.stream.Collectors;

public enum Screen {
    CHOOSE_DESTINATION {
        @Override
        public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                      BookingState state) {
            String destination = payload.getData().get("destination").toString();
            state.setDestination(destination);
            state.setStep(STEP_CHOOSE_DATE); //next screen

            List<Map<String, String>> dates = List.of(
                Map.of("id", "2025-06-10", "title", "Tue Jun 10 2025"),
                Map.of("id", "2025-06-11", "title", "Wed Jun 11 2025"),
                Map.of("id", "2025-06-12", "title", "Thu Jun 12 2025")
            );
            return new NextScreenResponsePayload(STEP_CHOOSE_DATE, Map.of(
                "destination", destination,
                "dates", dates
            ));
        }
    },
    CHOOSE_DATE {
        @Override
        public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                      BookingState state) {
            String date = payload.getData().get("date").toString();
            state.setDate(date);
            state.setStep(STEP_CHOOSE_TIME);
            Object[] times = {
                Map.of("id", "08:00", "title", "08:00 AM"),
                Map.of("id", "10:00", "title", "10:00 AM"),
                Map.of("id", "12:00", "title", "12:00 PM")
            };
            return new NextScreenResponsePayload(STEP_CHOOSE_TIME, Map.of(
                "destination", state.getDestination(),
                "date", date,
                "times", times
            ));
        }
    },
    CHOOSE_TIME {
        @Override
        public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                      BookingState state) {
            // 1) Extract chosen time
            String time = payload.getData().get("time").toString();
            state.setTime(time);

            // 2) Move to seat-selection step
            state.setStep(STEP_CHOOSE_SEAT);

            // 3) Load cached Base64 for the 10-seat image
            var imageCache = BeanUtil.getBean(ImageBase64Cache.class);
            String busBase64 = imageCache.getBase64("bus_10_seats");

            if (busBase64 == null) {
                // Show error if image not available
                Map<String,Object> err = Map.of(
                    "error_message", "🚧 Unable to load seat map right now."
                );
                return new NextScreenResponsePayload(STEP_CHOOSE_SEAT, err);
            }

            // 4) Prepare the dynamic data for that screen:
            //    - data.image = <Base64 string>
            //    - data.seats = [ {id:"A1",title:"A1"}, {id:"B1",title:"B1"}, … ]
            List<String> seatIds = List.of(
                "A1","B1","C1",
                "A2","B2","C2",
                "A3","B3",
                "A4","B4"
            );

            // 4a) Build List<Map<String,String>> where each map is { "id": seatId, "title": seatId }
            List<Map<String, Object>> seats = seatIds.stream()
                .map(id -> Map.of(
                        "id", id,
                        "title", id,
                    "on-select-action", Map.of("name", "update_data",
                        "enabled", true,
                        "payload", Map.of() ))

                )
                .collect(Collectors.toList());

            // 4b) Put into a single data map
            Map<String,Object> data = new LinkedHashMap<>();
            data.put("destination", state.getDestination());
            data.put("date",        state.getDate());
            data.put("time",        state.getTime());
            data.put("image", busBase64);
            data.put("seats", seats);


            // 5) Return payload whose `data` matches your JSON layout’s placeholders
            return new NextScreenResponsePayload(STEP_CHOOSE_SEAT, data);
        }
    },
    CHOOSE_SEAT {
        @Override
        public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                      BookingState state) {
            // 1) The user just tapped a chip (e.g. "B3")
            Object chosenSeats = payload.getData().get("seat");
            Collection<String> seats = (Collection<String>) chosenSeats;
            state.setChosenSeats(seats);

            // 2) Persist or mark the seat as taken if needed
            SeatService seatService = BeanUtil.getBean(SeatService.class);
            seats.forEach(seat -> {
                seatService.markSeatTaken(payload.getFlow_token(), seat);
            });


            // 3) Now advance to PASSENGER_INFO
            state.setStep(STEP_PASSENGER_INFORMATION);

            // 4) Build the PASSENGER_INFO payload (ask for name/email/phone/etc)
            //    This is exactly the same shape as before, e.g.:
            Map<String,Object> fields = new LinkedHashMap<>();
            fields.put("destination", state.getDestination());
            fields.put("date",        state.getDate());
            fields.put("time",        state.getTime());
            fields.put("seat",        chosenSeats);

            // The Flow builder (in your Companion JSON) expects something like:
            // { "type":"text_entry", "name":"full_name", "label":"Full Name" }, etc.
            // But if your PASSENGER_INFO is built via NextScreenResponsePayload, you might structure it as:
            //    new NextScreenResponsePayload("PASSENGER_INFO", fields);
            return new NextScreenResponsePayload(STEP_PASSENGER_INFORMATION, fields);
        }

        private Collection<String> toSeatCollection(Object chosenSeats) {
            Collection<String> seats;
            if (chosenSeats == null) {
                seats = Collections.emptyList();
            } else if (chosenSeats instanceof Collection<?> c) {
                seats = c.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
            } else {
                seats = Collections.singletonList(chosenSeats.toString());
            }
            return seats;
        }
    },
    PASSENGER_INFO {
        @Override
        public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                      BookingState state) {
            String fullName   = payload.getData().get("full_name").toString();
            String email      = payload.getData().get("email").toString();
            String phone      = payload.getData().get("phone").toString();
            String numTickets = payload.getData().get("num_tickets").toString();
            String moreDetails= payload.getData().getOrDefault("more_details", "").toString();

            // Update state
            state.setFullName(fullName);
            state.setEmail(email);
            state.setPhone(phone);
            state.setNumTickets(numTickets);
            state.setMoreDetails(moreDetails);
            state.setStep(STEP_SUMMARY);

            // Build SUMMARY screen data
            Map<String, Object> summaryData = new LinkedHashMap<>();
            summaryData.put("appointment",   formatAppointment(state));
            summaryData.put("details",       formatDetails(state));
            summaryData.put("destination",   state.getDestination());
            summaryData.put("date",          state.getDate());
            summaryData.put("time",          state.getTime());
            summaryData.put("seat",          state.getChosenSeats());
            summaryData.put("full_name",     fullName);
            summaryData.put("email",         email);
            summaryData.put("phone",         phone);
            summaryData.put("num_tickets",   numTickets);
            summaryData.put("more_details",  moreDetails);
            summaryData.put("summary_text",  buildSummaryText(summaryData));

            return new NextScreenResponsePayload(STEP_SUMMARY, summaryData);

        }
    },
    SUMMARY {
        @Override
        public FlowResponsePayload handleDataExchange(FlowDataExchangePayload payload,
                                                      BookingState state) {
            // Check if the user agreed to terms
            boolean agreed = Boolean.parseBoolean(
                payload.getData().getOrDefault("agree_terms", "false").toString()
            );

            if (!agreed) {
                // Re‐show SUMMARY with an error
                Map<String, Object> errorData = new LinkedHashMap<>();
                errorData.put("appointment",    formatAppointment(state));
                errorData.put("details",        formatDetails(state));
                errorData.put("error_message",  "❗ You must agree to the terms to proceed.");
                return new NextScreenResponsePayload(STEP_SUMMARY, errorData);
            }

            // Otherwise, finalize booking
            Map<String, Object> finalParams = new LinkedHashMap<>();
            finalParams.put("destination",  state.getDestination());
            finalParams.put("date",         state.getDate());
            finalParams.put("time",         state.getTime());
            finalParams.put("seat",          state.getChosenSeats());
            finalParams.put("full_name",    state.getFullName());
            finalParams.put("email",        state.getEmail());
            finalParams.put("phone",        state.getPhone());
            finalParams.put("num_tickets",  state.getNumTickets());
            finalParams.put("more_details", state.getMoreDetails());
            finalParams.put("flow_token",   payload.getFlow_token());

            var extMsgResponse = new FinalScreenResponsePayload.ExtensionMessageResponse(finalParams);
            return new FinalScreenResponsePayload(extMsgResponse);
        }
    };

    public static final String STEP_CHOOSE_SEAT = "CHOOSE_SEAT";
    public static final String STEP_CHOOSE_DATE = "CHOOSE_DATE";
    public static final String STEP_CHOOSE_TIME = "CHOOSE_TIME";
    public static final String STEP_PASSENGER_INFORMATION = "PASSENGER_INFO";
    public static final String STEP_SUMMARY = "SUMMARY";

    /**
     * Abstract method that each screen constant must implement.
     * @param requestPayload the decrypted FlowDataExchangePayload
     * @param state          the mutable BookingState
     * @return a FlowResponsePayload (either NextScreenResponsePayload or FinalScreenResponsePayload)
     */
    public abstract FlowResponsePayload handleDataExchange(
        FlowDataExchangePayload requestPayload, BookingState state
    );

    // ──────────────── SHARED HELPERS ────────────────
    private static String formatAppointment(BookingState state) {
        return "%s on %s at %s".formatted(
            state.getDestination(),
            state.getDate(),
            state.getTime()
        );
    }

    private static String formatDetails(BookingState state) {
        return "Name: %s%nEmail: %s%nPhone: %s%n\"%s\"".formatted(
            state.getFullName(),
            state.getEmail(),
            state.getPhone(),
            state.getMoreDetails() == null ? "" : state.getMoreDetails()
        );
    }

    private static String buildSummaryText(Map<String, Object> summaryData) {
        Object seatObj = summaryData.get("seat");
        String seatDisplay;

        if (seatObj == null) {
            seatDisplay = "Not selected";
        } else if (seatObj instanceof Collection<?>) {
            Collection<?> seats = (Collection<?>) seatObj;
            seatDisplay = seats.isEmpty() ? "Not selected" :
                seats.size() == 1 ? seats.iterator().next().toString() :
                    String.join(", ", seats.stream().map(Object::toString).toList());
        } else {
            seatDisplay = seatObj.toString();
        }

        return "*🗓 Appointment:* " + summaryData.get("appointment") + "\n" +
            "*📝 Details:* "     + summaryData.get("details")     + "\n\n" +
            "*📍 Destination:* " + summaryData.get("destination") + "\n" +
            "*📅 Date:* "        + summaryData.get("date")        + "\n" +
            "*⏰ Time:* "        + summaryData.get("time")        + "\n" +
            "*💺 Seat(s):* "         + seatDisplay        + "\n" +
            "*🎟 Tickets:* "     + summaryData.get("num_tickets") + "\n\n" +
            "_Any additional info:_ " + summaryData.get("more_details");
    }
}
