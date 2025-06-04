package com.tazifor.busticketing.util;

import com.tazifor.busticketing.service.BusLayoutService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * ImageOverlayUtil
 *
 * Draws semi‐transparent highlights on top of the cached bus layout (from BusLayoutService).
 * Uses the cached seatCoordinates (Map<String,Point>) and the base BufferedImage (in Base64).
 */
@Component
@RequiredArgsConstructor
public class ImageOverlayUtil {
    private final BusLayoutService busLayoutService;

    /** We’ll keep a direct reference to the “base” BufferedImage in memory */
    private BufferedImage baseImage;

    /** Cached seat → (x,y) map */
    private Map<String, Point> seatCoords;


    @PostConstruct
    public void init() throws Exception {
        // 1) Decode the Base64 from BusLayoutService back into a BufferedImage
        String b64 = busLayoutService.getBase64BusImage();
        if (b64 == null) {
            throw new IllegalStateException("No valid base64 bus image found");
        }
        byte[] bytes = Base64.getDecoder().decode(b64);
        this.baseImage = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        if (this.baseImage == null) {
            throw new IllegalStateException("Failed to decode base bus image");
        }

        // 2) Grab the seat‐coordinate map
        this.seatCoords = busLayoutService.getSeatCoordinates();
        if (seatCoords == null || seatCoords.isEmpty()) {
            throw new IllegalStateException("No seat coordinates found");
        }
    }

    /**
     * Given a list of selected seat IDs (e.g. ["A1","C2"]), return a new Base64‐encoded PNG
     * where each of those seats is highlighted with a semi-transparent overlay.
     *
     * @param selectedSeats list of seat IDs to highlight
     * @return "data:image/png;base64, …" new PNG string
     */
    public String createImageWithHighlights(List<String> selectedSeats) {
        if (baseImage == null) {
            throw new IllegalStateException("Base bus image not initialized");
        }

        // 3) Copy the base image so we don’t modify it
        BufferedImage copy = new BufferedImage(
            baseImage.getWidth(),
            baseImage.getHeight(),
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = copy.createGraphics();
        g.drawImage(baseImage, 0, 0, null);

        // 4) Draw a semi-transparent red circle (highlight) around each seat’s stored coordinates
        g.setColor(new Color(255, 0, 0, 128));  // red with 50% opacity
        int highlightRadius =  (int)(busLayoutService.getSeatCoordinates().values().iterator().next().getX() * 0 + 20);
        // We pick 20 px here as a generic radius;
        // if you want a different diameter, you could read it from BusLayoutConfig.

        for (String seatId : selectedSeats) {
            Point p = seatCoords.get(seatId);
            if (p == null) continue; // skip invalid keys
            int cx = p.x - (highlightRadius / 2);
            int cy = p.y - (highlightRadius / 2);
            g.fillOval(cx, cy, highlightRadius, highlightRadius);
        }
        g.dispose();

        // 5) Encode copy to Base64
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(copy, "PNG", baos);
            String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + b64;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to encode overlaid image: " + ex.getMessage(), ex);
        }
    }
}
