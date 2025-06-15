package com.tazifor.busticketing.util;

import com.tazifor.busticketing.service.BusLayoutService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Cached seat boundary→ (x,y) map */
    private Map<String, Rectangle> seatBounds;


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

        // 3) Grab seat boundaries
        this.seatBounds = busLayoutService.getSeatBounds();
        if (seatBounds == null || seatBounds.isEmpty()) {
            throw new IllegalStateException("No seat bounds found");
        }
    }

    /**
     * Highlights which seats are available (green) vs taken (gray), with a legend.
     */
    public String createAvailabilityOverlay(Set<String> availableSeats) {
        BufferedImage copy = getBaseCopy();
        Graphics2D g = copy.createGraphics();

        // Available seats: green
        Color availC = new Color(0, 128, 0, 100);
        availableSeats.forEach(id -> fillSeatWithColor(g, id, availC));

        // Taken seats: gray
        Color takenC = new Color(128, 128, 128, 100);
        seatBounds.keySet().stream()
            .filter(id -> !availableSeats.contains(id))
            .forEach(id -> fillSeatWithColor(g, id, takenC));

        addLegend(g, Map.of(
            "Available", new Color(0, 128, 0, 180),
            "Taken", new Color(128, 128, 128, 180)
        ));
        g.dispose();
        return encodeToBase64(copy);
    }

    /**
     * Given a list of selected seat IDs (e.g. ["A1","C2"]), return a new Base64‐encoded PNG
     * where each of those seats is highlighted with a semi-transparent overlay, with a legend.
     *
     * @param selectedSeats list of seat IDs to highlight
     * @return "base64, …" new PNG string
     */
    public String createImageWithHighlights(List<String> selectedSeats) {
        BufferedImage copy = getBaseCopy();
        Graphics2D g = copy.createGraphics();

        Color selC = new Color(255, 0, 0, 128);
        selectedSeats.forEach(id -> fillSeatWithColor(g, id, selC));

        addLegend(g, Map.of("Selected", new Color(255, 0, 0, 180)));
        g.dispose();
        return encodeToBase64(copy);
    }

    /* clones the base layout image and returns it as a BufferedImage.*/
    private BufferedImage getBaseCopy() {
        BufferedImage copy = new BufferedImage(
            baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg = copy.createGraphics();
        cg.drawImage(baseImage, 0, 0, null);
        cg.dispose();
        return copy;
    }
    /* draws the colored overlay at a seat’s coordinates.*/
    private void fillSeatWithColor(Graphics2D g, String seatId, Color color) {
        Rectangle rect = seatBounds.get(seatId);
        if (rect != null) {
            g.setColor(color);
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
    }
    /* draws your seat color legend in the bottom-left.*/
    private void addLegend(Graphics2D g, Map<String, Color> legendItems) {
        int pad = 10, boxSize = 15;
        int x = pad, y = baseImage.getHeight() - legendItems.size() * (boxSize + 5) - pad;
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));

        for (Map.Entry<String, Color> entry : legendItems.entrySet()) {
            g.setColor(entry.getValue());
            g.fillRect(x, y, boxSize, boxSize);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, boxSize, boxSize);
            g.drawString(entry.getKey(), x + boxSize + 5, y + boxSize - 3);
            y += boxSize + 5;
        }
    }
    /* encodes the final image for sending via WhatsApp.*/
    private String encodeToBase64(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", baos);
            //return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to encode image", ex);
        }
    }

}
