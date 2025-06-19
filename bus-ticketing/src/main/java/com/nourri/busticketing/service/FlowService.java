package com.nourri.busticketing.service;


import com.nourri.busticketing.dto.*;
import com.nourri.busticketing.dto.crypto.FlowEncryptedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nourri.busticketing.exception.BookingProcessingException;
import com.nourri.busticketing.model.*;
import com.nourri.busticketing.service.screens.ScreenHandler;
import com.nourri.busticketing.util.encoding.BookingStateCodec;
import com.nourri.busticketing.util.StateDiffUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Orchestrates both encrypted (endpoint‐powered) and unencrypted interactive Flows.
 */
@Service
@RequiredArgsConstructor
public class FlowService {
    private static final Logger logger = LoggerFactory.getLogger(FlowService.class);

    private final com.nourri.busticketing.service.FlowEncryptionService encryptionService;
    private final Map<String, ScreenHandler>  screenHandlers;
    private final ObjectMapper objectMapper;
    private final com.nourri.busticketing.service.TicketSendingService ticketSendingService;
    private final com.nourri.busticketing.service.BookingService bookingService;
    private final com.nourri.busticketing.service.AgencyService agencyService;

    /**
     * Decrypts the incoming encrypted payload (FlowEncryptedPayload), runs flow logic, re‐encrypts the new state,
     * and returns a Map suitable for serialization back to WhatsApp’s Flow data‐exchange protocol.
     * Endpoint path: POST /webhook/flow/data-exchange
     *
     * @param encryptedPayload the raw encrypted input from WhatsApp
     * @return a Mono emitting the full response map (with keys "version", "flow_token", "data", "screen", and optional "close")
     */
    public Mono<String> handleExchange(FlowEncryptedPayload encryptedPayload) {
        return Mono.fromCallable(() -> {
            // 1) Decrypt incoming payload (AES data + RSA‐wrapped key + IV)
            FlowEncryptionService.DecryptionResult dr = encryptionService.decryptPayload(
                    encryptedPayload.getEncryptedFlowData(),
                    encryptedPayload.getEncryptedAesKey(),
                    encryptedPayload.getInitialVector()
            );

            // 2) Parse decrypted JSON into a typed request
            FlowDataExchangePayload decryptedRequestPayload = objectMapper.readValue(
                    dr.clearJson(),
                    FlowDataExchangePayload.class
            );

            String action = decryptedRequestPayload.getAction();
            logger.info("Flow action {}", action);

            // 3) If "ping", return encrypted health‐check
            if ("ping".equals(action)) {
                String healthJson = objectMapper.writeValueAsString(
                        Map.of("data", Map.of("status", "active"))
                );
                return encryptionService.encryptPayload(
                        healthJson, dr.aesKey(), dr.iv()
                );
            }

            // 4) Rebuild or initialize the domain state
            BookingState state = Optional.ofNullable(decryptedRequestPayload.getData().get("_state"))
                .map(encoded -> BookingStateCodec.decode(encoded.toString()))
                .orElse(BookingState.empty());

            // 5) Decide which UI screen to show next (or final)
            ScreenHandlerResult screenHandlerResult;
            switch (action) {
                case "INIT":
                    logger.info("INIT for token {}", decryptedRequestPayload.getFlow_token());
                    screenHandlerResult = Screen.buildInitialScreen(state);
                    break;
                case "BACK":
                    screenHandlerResult = Screen.showBackScreen(state);
                    break;
                case "data_exchange":
                    // Look up enum by req.getScreen() and invoke its handle(...)
                    String currentScreen = decryptedRequestPayload.getScreen();
                    logger.info("data_exchange for screen {}", currentScreen);
                    logger.info("decryptedRequestPayload {}", decryptedRequestPayload);

                    if(StringUtils.hasLength(currentScreen) || !screenHandlers.containsKey(currentScreen)){
                        if (decryptedRequestPayload.getData().containsKey("error")) {
                            screenHandlers.get("GENERIC_ERROR");
                        }
                    }

                    ScreenHandler screenHandler = screenHandlers.get(currentScreen);
                    screenHandlerResult = screenHandler.handleDataExchange(decryptedRequestPayload, state);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown action: " + action);
            }

            BookingState newState = screenHandlerResult.newState();
            FlowResponsePayload flowResponsePayload = screenHandlerResult.response();
            // 6) If it’s a final payload, ensure flow_token is included in the ExtensionMessageResponse
            if (flowResponsePayload instanceof FinalScreenResponsePayload finalUi) {
                ((FinalScreenResponsePayload.ExtensionMessageResponse)finalUi.getData().get("extension_message_response")).validate();
            }else {
                flowResponsePayload = ((NextScreenResponsePayload) flowResponsePayload).withState(newState);
            }

            logger.info("State transition: [{}] → [{}]\n{}\nState\n-----\n{}",
                state.getStep(),
                newState.getStep(),
                StateDiffUtil.prettyPrintDiff(state, newState),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(newState)
            );

            // 7) Serialize and re‐encrypt the new state
            String uiAsString = objectMapper.writeValueAsString(flowResponsePayload);
            return encryptionService.encryptPayload(uiAsString, dr.aesKey(), dr.iv());

        });
    }

    /**
     * Called when a Flow reaches its final “Complete” action.
     * You receive whatever arbitrary JSON was defined in the Complete action under response_json.
     *
     * @param finalParams Map of final fields, e.g. { "flow_token": "...", "appointment_date": "...", etc. }
     * @param from        The user’s phone number (no “+”), so you can send a confirmation message.
     */
    public void handleFlowCompletion(Map<String, Object> finalParams, String from) {
        String flowToken = finalParams.getOrDefault("flow_token", "").toString();

        logger.info("Plain Flow completed for token {} from {} with params {}", flowToken, from, finalParams);

        try {
            Booking booking = createBookingFromParams(finalParams, from);
            logger.info("Created booking with ID {}", booking.getId());
            String agencyPhone = finalParams.getOrDefault("agency_phone", "").toString();
            List<RenderableTicket> tickets = buildRenderableTickets(booking, agencyPhone);
            logger.info("Built {} tickets", tickets.size());
            ticketSendingService.sendAllTickets(from, tickets);
        } catch (Exception e) {
            logger.error("Failed to process booking completion", e);
            throw new BookingProcessingException("Failed to complete booking", e);
        }
    }

    private Booking createBookingFromParams(Map<String, Object> finalParams, String customerPhone) {
        // Extract required parameters with validation
        UUID scheduleId = UUID.fromString(finalParams.get("schedule_id").toString());

        // Process passengers
        List<Passenger> passengers = Optional.ofNullable((List<Object>) finalParams.get("passengers"))
            .orElseGet(List::of)
            .stream()
            .filter(Map.class::isInstance)
            .map(obj -> {
                try {
                    return objectMapper.convertValue(obj, Passenger.class);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid passenger data: {}", obj);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();

        if (passengers.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required");
        }

        // Extract booking details
        String customerName = passengers.get(0).getName();
        String email = passengers.get(0).getEmail();
        String moreDetails = finalParams.getOrDefault("more_details", "").toString();
        List<String> seatNumbers = Optional.ofNullable((List<String>) finalParams.get("seat_numbers"))
            .orElseGet(List::of);
        List<String> passengerNames = passengers.stream()
            .map(Passenger::getName)
            .toList();

        return bookingService.createBooking(
            customerName,
            customerPhone, // TODO: Use the provided phone number
            email,
            moreDetails,
            scheduleId,
            seatNumbers,
            passengerNames
        );
    }


    private List<RenderableTicket> buildRenderableTickets(Booking booking, String agencyPhone) {
        if (booking.getTickets() == null || booking.getTickets().isEmpty()) {
            return List.of();
        }

        // Get common data from first ticket
        Ticket firstTicket = booking.getTickets().get(0);
        Schedule schedule = firstTicket.getSchedule();

        Agency agency = schedule.getAgency();
        Location from = schedule.getFromLocation();
        Location to = schedule.getToLocation();

        // Calculate price - get from schedule's class prices
        ScheduleClassPrice classPrice = schedule.getScheduleClassPrices().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No class prices found for schedule " + schedule.getId()));
        int basePrice = classPrice.getPrice();
        logger.debug("Schedule {} has base price {}", schedule.getId(), basePrice);
        String travelClass = classPrice.getTravelClass().getName();

        boolean isRoundTrip = Boolean.TRUE.equals(booking.getIsRoundTrip());
        int finalPrice = isRoundTrip ? (int)(basePrice * 2 * 0.85) : basePrice;

        return booking.getTickets().stream().map(ticket -> {
            RenderableTicket renderable = new RenderableTicket();

            // Set ticket details
            renderable.setTicketNumber("TX-" + ticket.getId().toString().substring(0, 8).toUpperCase());
            renderable.setPassengerName(ticket.getPassengerName());
            renderable.setPassengerEmail(ticket.getPassengerEmail());
            renderable.setPassengerPhone(ticket.getPassengerPhone());

            // Set seat number directly from ticket
            if (ticket.getSeatNumber() != null && !ticket.getSeatNumber().isEmpty()) {
                renderable.setSeat(ticket.getSeatNumber());
            }

            // Set schedule info
            renderable.setOrigin(from.getName());
            renderable.setDestination(to.getName());
            renderable.setDate(schedule.getTravelDate().toString());
            renderable.setTime(schedule.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm")));

            // Set agency info
            renderable.setAgency(agency.getName());
            renderable.setAgencyPhone(agencyPhone);

            // Determine travel class - needs to come from schedule's class prices
            renderable.setTravelClass(travelClass);

            renderable.setPrice(finalPrice);
            renderable.setRoundTrip(isRoundTrip);
            renderable.setIssuedAt(LocalDateTime.now());

            return renderable;
        }).toList();
    }


}

