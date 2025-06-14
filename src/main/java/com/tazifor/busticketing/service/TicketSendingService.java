package com.tazifor.busticketing.service;

import com.tazifor.busticketing.client.WhatsAppApiClient;
import com.tazifor.busticketing.model.Ticket;
import com.tazifor.busticketing.util.ImageOverlayUtil;
import com.tazifor.busticketing.util.TicketFormatter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TicketSendingService {
    private static final Logger logger = LoggerFactory.getLogger(TicketSendingService.class);

    private final WhatsAppApiClient apiClient;
    private final ImageOverlayUtil overlayUtil;

    public void sendAllTickets(String toPhone, List<Ticket> tickets) {
        boolean seatsChosen = tickets.stream()
            .anyMatch(t -> t.getSeat() != null && !t.getSeat().isBlank());

        String seatOverlayBase64 = null;
        if (seatsChosen) {
            List<String> allSeats = tickets.stream()
                .map(Ticket::getSeat)
                .filter(Objects::nonNull)
                .toList();
            seatOverlayBase64 = overlayUtil.createImageWithHighlights(allSeats);
        }

        for (int i = 0; i < tickets.size(); i++) {
            Ticket t = tickets.get(i);
            String caption = TicketFormatter.formatTicketCaption(t);

            if (seatOverlayBase64 != null && i == 0) {
                apiClient.uploadAndSendBase64Image(toPhone, seatOverlayBase64, caption)
                    .subscribe(
                        __ -> logger.info("Sent seat map ticket to {}", toPhone),
                        err -> logger.error("Error sending ticket image", err)
                    );
            } else {
                apiClient.sendText(toPhone, caption)
                    .subscribe(
                        __ -> logger.info("Sent ticket to {}", toPhone),
                        err -> logger.error("Error sending ticket text", err)
                    );
            }
        }
    }
}

