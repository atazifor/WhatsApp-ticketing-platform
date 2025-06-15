package com.tazifor.busticketing.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

/**
 * BusLayoutService
 *
 * 1) On startup, call BusLayoutGenerator.generateLayout()
 * 2) From the returned BufferedImage, produce a Base64‐encoded PNG string.
 * 3) Cache both the seatCoordinates map and the base64BusImage string for others to fetch.
 */
@Service
@RequiredArgsConstructor
public class BusLayoutService {
    private static final Logger log = LoggerFactory.getLogger(BusLayoutService.class);

    private final BusLayoutGenerator generator;

    /** After initialization, holds seatID→center‐Point (e.g. "A1"→(x,y)). */
    @Getter
    private Map<String, Point> seatCoordinates;
    @Getter
    private Map<String, Rectangle> seatBounds;

    /** After initialization, holds “data:image/png;base64,…” for the bus layout. */
    @Getter
    private String base64BusImage;

    @PostConstruct
    public void initialize() {
        try {
            // 1) Let the generator build the image + seat‐centers
            BusLayoutGenerator.GenerationResult result = generator.generateLayout();
            BufferedImage busImage = result.image();
            this.seatCoordinates = result.seatCenters();
            seatBounds = result.seatBounds();

            // 2) Encode busImage → PNG → Base64
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(busImage, "PNG", baos);
                this.base64BusImage = Base64.getEncoder().encodeToString(baos.toByteArray());
            }

            log.info("BusLayoutService initialized: {} seats cached, base64 image length={}",
                seatCoordinates.size(),
                base64BusImage.length()
            );

        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize BusLayoutService", ex);
        }
    }

}
