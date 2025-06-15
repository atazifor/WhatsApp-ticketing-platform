package com.tazifor.busticketing.service;

import com.tazifor.busticketing.config.properties.BusLayoutConfig;
import com.tazifor.busticketing.model.BusLayoutCellType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
    * Generates a bus layout image and computes seat-center coordinates.
    * Seat numbering increments sequentially (1,2,3…), ignoring driver/entrance/etc.
 */
@Component
public class BusLayoutGenerator {
    private static final Logger log = LoggerFactory.getLogger(BusLayoutGenerator.class);
    private final BusLayoutConfig cfg;

    public BusLayoutGenerator(BusLayoutConfig cfg) {
        this.cfg = cfg;
    }

    public GenerationResult generateLayout() {
        // --- 1) Read configuration ---
        int rows       = cfg.rows();
        int colsLeft   = cfg.colsLeft();
        int colsRight  = cfg.colsRight();
        int totalCols  = colsLeft + colsRight;
        int seatSize   = cfg.seatSize();
        int seatGap    = cfg.seatGap();
        int aisleWidth= cfg.aisleWidth();
        int padding    = cfg.padding();
        int driverH    = cfg.driverAreaHeight();
        int labelH     = cfg.labelHeight();

        List<List<BusLayoutCellType>> layoutMap = cfg.layoutMap();
        if (layoutMap == null || layoutMap.size() != rows) {
            throw new IllegalStateException("layoutMap must have exactly " + rows + " rows");
        }
        for (List<BusLayoutCellType> row : layoutMap) {
            if (row.size() != totalCols) {
                throw new IllegalStateException("Each row in layoutMap must have length = colsLeft + colsRight = " + totalCols);
            }
        }

        // --- 2) Calculate image dimensions ---
        int leftBlockWidth  = colsLeft * seatSize + (colsLeft - 1) * seatGap;
        int rightBlockWidth = colsRight * seatSize + (colsRight - 1) * seatGap;
        int width  = padding * 2 + leftBlockWidth + seatGap + aisleWidth + seatGap + rightBlockWidth;
        int height = padding + labelH + driverH + seatGap
            + rows * seatSize + (rows - 1) * seatGap + padding;

        log.info("BusLayoutGenerator: creating image {}×{} (rows={}, cols={})", width, height, rows, totalCols);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        Map<String, Point> seatCenterMap   = new LinkedHashMap<>();
        Map<String, Rectangle> seatBoundsMap = new LinkedHashMap<>();

        // --- 3) Draw each cell ---
        int seatCounter = 0;
        for (int r = 0; r < rows; r++) {
            int rowY = (r == 0)
                ? padding
                : padding + labelH + driverH + seatGap + (r - 1) * (seatSize + seatGap);

            for (int c = 0; c < totalCols; c++) {
                int cellX = (c < colsLeft)
                    ? padding + c * (seatSize + seatGap)
                    : padding + leftBlockWidth + seatGap + aisleWidth + seatGap
                    + (c - colsLeft) * (seatSize + seatGap);

                BusLayoutCellType type = layoutMap.get(r).get(c);
                Integer mySeatNum = null;
                if (type == BusLayoutCellType.SEAT) {
                    seatCounter++;
                    mySeatNum = seatCounter;
                }

                drawCellAndRecord(g, type, mySeatNum, cellX, rowY,
                    seatSize, driverH, seatSize,
                    seatCenterMap, seatBoundsMap);
            }
        }

        g.dispose();
        return new GenerationResult(img, seatCenterMap, seatBoundsMap);
    }

    // Draw one cell; if it's a seat, label with sequential number and record map entries:
    private void drawCellAndRecord(
        Graphics2D g,
        BusLayoutCellType type,
        Integer seatNum,
        int x, int y,
        int seatSize,
        int driverH,
        int toiletSize,
        Map<String, Point> seatCenterMap,
        Map<String, Rectangle> seatBoundsMap
    ) {
        String cellId = (seatNum != null) ? seatNum.toString() : null;

        switch (type) {
            case DRIVER -> drawDriver(g, x, y, seatSize, driverH);
            case ENTRANCE -> drawEntrance(g, x, y, seatSize, driverH);
            case SEAT -> {
                g.setColor(Color.WHITE);
                g.fillRect(x, y, seatSize, seatSize);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, seatSize, seatSize);
                drawSeatLabel(g, cellId, x, y, seatSize);
                seatCenterMap.put(cellId, new Point(x + seatSize / 2, y + seatSize / 2));
                seatBoundsMap.put(cellId, new Rectangle(x, y, seatSize, seatSize));
            }
            case TOILET -> drawToilet(g, x, y, toiletSize);
            case EMPTY -> {
                g.setColor(new Color(245, 245, 245));
                g.fillRect(x, y, seatSize, seatSize);
                g.setColor(Color.GRAY);
                g.drawRect(x, y, seatSize, seatSize);
            }
        }
    }

    private void drawSeatLabel(Graphics2D g, String label, int x, int y, int size) {
        Font seatFont = new Font("SansSerif", Font.BOLD, 14);
        g.setFont(seatFont);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textX = x + (size - textWidth) / 2;
        int textY = y + (size + fm.getAscent()) / 2 - 4;
        g.setColor(Color.BLACK);
        g.drawString(label, textX, textY);
    }

    private void drawDriver(Graphics2D g, int x, int y, int seatSize, int driverH) {
        g.setColor(new Color(200, 230, 255));
        g.fillRect(x, y, seatSize, driverH);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, seatSize, driverH);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        String label = "Driver";
        g.drawString(label, x + (seatSize - fm.stringWidth(label)) / 2,
            y + (driverH + fm.getAscent()) / 2 - 4);
    }

    private void drawEntrance(Graphics2D g, int x, int y, int seatSize, int driverH) {
        g.setColor(new Color(200, 255, 200));
        g.fillRect(x, y, seatSize, driverH);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, seatSize, driverH);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        String label = "Entrance";
        g.drawString(label, x + (seatSize - fm.stringWidth(label)) / 2,
            y + (driverH + fm.getAscent()) / 2 - 4);
    }

    private void drawToilet(Graphics2D g, int x, int y, int toiletSize) {
        g.setColor(new Color(255, 230, 230));
        g.fillRect(x, y, toiletSize, toiletSize);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, toiletSize, toiletSize);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fm = g.getFontMetrics();
        String label = "Toilet";
        g.drawString(label, x + (toiletSize - fm.stringWidth(label)) / 2,
            y + (toiletSize + fm.getAscent()) / 2 - 4);
    }

    /** Holds the generated image and lookup maps */
    public record GenerationResult(
        BufferedImage image,
        Map<String, Point> seatCenters,
        Map<String, Rectangle> seatBounds
    ) {}
}