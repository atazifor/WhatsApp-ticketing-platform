package com.nourri.busticketing.generator;



import com.nourri.busticketing.LayoutAssets;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

// In bus-layout module
public class BusLayoutOverlay {
    private final LayoutAssets layoutAssets;
    private final Font legendFont;

    public BusLayoutOverlay(LayoutAssets layoutAssets, Font legendFont) {
        this.layoutAssets = Objects.requireNonNull(layoutAssets, "LayoutAssets cannot be null");
        this.legendFont = Objects.requireNonNull(legendFont, "Legend font cannot be null");
    }

    /**
     * Creates an overlay showing available vs taken seats
     */
    public String createAvailabilityOverlay(Set<String> availableSeats) {
        BufferedImage copy = getBaseCopy();
        Graphics2D g = copy.createGraphics();

        // Available seats: green
        Color availC = new Color(0, 128, 0, 100);
        availableSeats.forEach(id -> fillSeatWithColor(g, id, availC));

        // Taken seats: gray
        Color takenC = new Color(128, 128, 128, 100);
        layoutAssets.seatBounds().keySet().stream()
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
     * Highlights selected seats with red overlay
     */
    public String createImageWithHighlights(List<String> selectedSeats) {
        BufferedImage base = getBaseCopy();
        Graphics2D baseG = base.createGraphics();
        baseG.setFont(legendFont);
        FontMetrics fm = baseG.getFontMetrics();
        baseG.dispose();

        int fontHeight = fm.getHeight();
        int legendHeight = (fontHeight + fontHeight/3) * 1 + fontHeight;

        BufferedImage extended = new BufferedImage(
            base.getWidth(),
            base.getHeight() + legendHeight,
            BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = extended.createGraphics();
        g.drawImage(base, 0, 0, null);

        Color selC = new Color(255, 0, 0, 128);
        selectedSeats.forEach(id -> fillSeatWithColor(g, id, selC));

        addLegend(g, Map.of("Selected", new Color(255, 0, 0, 180)));
        g.dispose();
        return encodeToBase64(extended);
    }

    /* clones the base layout image and returns it as a BufferedImage.*/
    private BufferedImage getBaseCopy() {
        BufferedImage baseImage = layoutAssets.image();
        BufferedImage copy = new BufferedImage(
            baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D cg = copy.createGraphics();
        cg.drawImage(baseImage, 0, 0, null);
        cg.dispose();
        return copy;
    }
    /* draws the colored overlay at a seat’s coordinates.*/
    private void fillSeatWithColor(Graphics2D g, String seatId, Color color) {
        Rectangle rect = layoutAssets.seatBounds().get(seatId);
        if (rect != null) {
            g.setColor(color);
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        }
    }
    /* draws your seat color legend in the bottom-left.*/
    private void addLegend(Graphics2D g, Map<String, Color> legendItems) {
        // Set the font and metrics
        g.setFont(legendFont);
        FontMetrics fm = g.getFontMetrics();

        // Dynamically scale everything based on font height
        int fontHeight = fm.getHeight();
        int boxSize = fontHeight;
        int pad = fontHeight / 2;
        int gap = fontHeight / 3;

        // Calculate max text width across legend labels
        int maxLabelWidth = legendItems.keySet().stream()
            .mapToInt(fm::stringWidth)
            .max().orElse(0);

        // Width = color box + space + label
        int legendWidth = boxSize + pad + maxLabelWidth;
        int legendHeight = legendItems.size() * (boxSize + gap);

        // Calculate bottom of actual seat layout to avoid overlap
        int maxSeatBottom = layoutAssets.seatBounds().values().stream()
            .mapToInt(rect -> rect.y + rect.height)
            .max().orElse(0);

        int x = pad;
        int y = maxSeatBottom + pad;

        // Optional: semi-transparent background
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRoundRect(x - pad, y - pad, legendWidth + pad * 2, legendHeight + pad * 2, pad, pad);

        // Now draw each legend entry
        for (Map.Entry<String, Color> entry : legendItems.entrySet()) {
            g.setColor(entry.getValue());
            g.fillRect(x, y, boxSize, boxSize);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, boxSize, boxSize);
            g.drawString(entry.getKey(), x + boxSize + pad, y + boxSize - gap);
            y += boxSize + gap;
        }
    }

    /* encodes the final image for sending via WhatsApp.*/
    private String encodeToBase64(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to encode image", ex);
        }
    }
}
