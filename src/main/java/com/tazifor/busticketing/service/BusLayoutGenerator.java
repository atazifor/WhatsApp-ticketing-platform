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
 * Generates a bus‐layout image and computes seat‐center coordinates given a BusLayoutConfig.
 *
 * Responsibility:
 *   1) Read all layout parameters (rows, colsLeft, colsRight, seatSize, etc.).
 *   2) Construct a BufferedImage (TYPE_INT_ARGB) of the correct width×height.
 *   3) Draw each “cell” (DRIVER, ENTRANCE, SEAT, TOILET, EMPTY) in its proper x/y position.
 *   4) Whenever a SEAT cell is drawn, record its center coordinates into a Map<String,Point>.
 *
 * After drawing, wrap the BufferedImage and the seat‐coordinate map into a small GenerationResult object.
 */
@Component
public class BusLayoutGenerator {
    private static final Logger log = LoggerFactory.getLogger(BusLayoutGenerator.class);

    private final BusLayoutConfig cfg;

    public BusLayoutGenerator(BusLayoutConfig cfg) {
        this.cfg = cfg;
    }

    /**
     * Performs the entire “read config → draw image → compute seat coordinates” flow.
     * @return a GenerationResult containing the drawn BufferedImage and the seat→center‐Point map
     */
    public GenerationResult generateLayout() {
        try {
            // ─────────── 1) Pull all values from cfg ─────────────────────────
            int rows         = cfg.rows();          // total rows (including driver row)
            int colsLeft     = cfg.colsLeft();      // seats on left side per row
            int colsRight    = cfg.colsRight();     // seats (or toilet/empty) on right side per row
            int seatSize     = cfg.seatSize();      // in pixels (width=height)
            int seatGap      = cfg.seatGap();       // horizontal & vertical gap between cells
            int aisleWidth   = cfg.aisleWidth();    // width of the center aisle in px
            int padding      = cfg.padding();       // outer padding (top/left/bottom/right)
            int driverH      = cfg.driverAreaHeight(); // height of “driver” & “entrance” row
            int labelH       = cfg.labelHeight();   // space reserved above driver area for labels

            List<List<BusLayoutCellType>> layoutMap = cfg.layoutMap();
            if (layoutMap == null || layoutMap.size() != rows) {
                throw new IllegalStateException("layoutMap must have exactly " + rows + " rows");
            }
            for (List<BusLayoutCellType> row : layoutMap) {
                if (row.size() != colsLeft + colsRight) {
                    throw new IllegalStateException(
                        "Each row in layoutMap must have length = colsLeft + colsRight = "
                            + (colsLeft + colsRight)
                    );
                }
            }

            // ─────────── 2) Compute the overall image dimensions ──────────────
            // Left block width = colsLeft * seatSize + (colsLeft - 1) * seatGap
            int leftBlockWidth = colsLeft * seatSize + (colsLeft - 1) * seatGap;
            // Right block width = colsRight * seatSize + (colsRight - 1) * seatGap
            int rightBlockWidth = colsRight * seatSize + (colsRight - 1) * seatGap;
            // Total width = padding*2 + leftBlock + seatGap + aisleWidth + seatGap + rightBlock
            int width = padding * 2
                + leftBlockWidth
                + seatGap + aisleWidth + seatGap
                + rightBlockWidth;

            // Total height = padding + labelH + driverH + seatGap
            //              + rows*seatSize + (rows - 1)*seatGap + padding
            int height = padding
                + labelH
                + driverH
                + seatGap
                + rows * seatSize
                + (rows - 1) * seatGap
                + padding;

            log.info("BusLayoutGenerator: creating image {}×{} (rows={}, colsLeft={}, colsRight={})",
                width, height, rows, colsLeft, colsRight);

            // ─────────── 3) Create a blank BufferedImage and Graphics2D ─────────
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            // We'll collect seat→center‐Point here
            Map<String, Point> seatCenterMap = new LinkedHashMap<>();

            try {
                // 3a) Antialias text
                g.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                );

                // 3b) Fill background white
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);

                // 3c) Iterate every row r=0..rows-1 and column c=0..(colsLeft+colsRight-1)
                for (int r = 0; r < rows; r++) {
                    // Compute the Y coordinate for this row:
                    //  - For row 0: y = padding
                    //  - For r>0: y = padding + labelH + driverH + seatGap + (r-1)*(seatSize + seatGap)
                    int rowY = (r == 0)
                        ? padding
                        : padding + labelH + driverH + seatGap + (r - 1) * (seatSize + seatGap);

                    for (int c = 0; c < colsLeft + colsRight; c++) {
                        // Compute the X coordinate:
                        int cellX;
                        if (c < colsLeft) {
                            // left side
                            cellX = padding + c * (seatSize + seatGap);
                        } else {
                            // right side (after left block + seatGap + aisleWidth + seatGap)
                            int rightIndex = c - colsLeft; // 0..(colsRight-1)
                            cellX = padding
                                + leftBlockWidth
                                + seatGap
                                + aisleWidth
                                + seatGap
                                + rightIndex * (seatSize + seatGap);
                        }

                        // Fetch the cell‐type from layoutMap
                        BusLayoutCellType type = layoutMap.get(r).get(c);

                        // Call helper to draw that one cell and record seat centers if needed
                        drawCellAndRecord(
                            g,
                            type,
                            r, c,
                            cellX,
                            rowY,
                            seatSize,
                            driverH,
                            seatSize,
                            seatCenterMap
                        );
                    }
                }
            } finally {
                g.dispose();
            }

            return new GenerationResult(img, seatCenterMap);

        } catch (Exception ex) {
            throw new RuntimeException("BusLayoutGenerator failed", ex);
        }
    }

    /**
     * Draw exactly one cell at (x,y). If it’s a SEAT type, label it “<rowLetter><colNumber>”
     * and store its center coordinates into seatCenterMap.
     *
     * @param g                Graphics2D context
     * @param type             one of DRIVER, ENTRANCE, SEAT, TOILET, EMPTY
     * @param rowIndex         0-based (0 = driver row)
     * @param colIndex         0-based
     * @param x                top-left X for this cell
     * @param y                top-left Y for this cell
     * @param seatSize         width & height of a seat/toilet/empty cell
     * @param driverH          height for DRIVER/ENTRANCE cells (only for rowIndex == 0)
     * @param toiletSize       same as seatSize
     * @param seatCenterMap    map to populate if type == SEAT
     */
    private void drawCellAndRecord(
        Graphics2D g,
        BusLayoutCellType type,
        int rowIndex,
        int colIndex,
        int x, int y,
        int seatSize,
        int driverH,
        int toiletSize,
        Map<String, Point> seatCenterMap
    ) {
        // Build a “seat ID” string for labeling:
        // Row letter = 'A' + rowIndex  → “A” for row 0, “B” for row 1, etc.
        String rowLetter = Character.toString((char) ('A' + rowIndex));
        String cellId    = rowLetter + (colIndex + 1); // e.g. “A1”, “B3”, etc.

        switch (type) {
            case DRIVER -> {
                // Draw a light‐blue driver rectangle
                g.setColor(new Color(200, 230, 255));
                g.fillRect(x, y, seatSize, driverH);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, seatSize, driverH);

                // Label “Driver” centered
                Font driverFont = new Font("SansSerif", Font.BOLD, 16);
                g.setFont(driverFont);
                FontMetrics dfm = g.getFontMetrics();
                String label = "Driver";
                int textX = x + (seatSize - dfm.stringWidth(label)) / 2;
                int textY = y + (driverH + dfm.getAscent()) / 2 - 4;
                g.drawString(label, textX, textY);
            }
            case ENTRANCE -> {
                // Light‐green entrance rectangle
                g.setColor(new Color(200, 255, 200));
                g.fillRect(x, y, seatSize, driverH);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, seatSize, driverH);

                // Label “Entrance” centered
                Font entFont = new Font("SansSerif", Font.BOLD, 16);
                g.setFont(entFont);
                FontMetrics efm = g.getFontMetrics();
                String label = "Entrance";
                int textX = x + (seatSize - efm.stringWidth(label)) / 2;
                int textY = y + (driverH + efm.getAscent()) / 2 - 4;
                g.drawString(label, textX, textY);
            }
            case SEAT -> {
                // White seat with black border
                g.setColor(Color.WHITE);
                g.fillRect(x, y, seatSize, seatSize);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, seatSize, seatSize);

                // Draw seat label (e.g. “B3”) centered
                drawSeatLabel(g, cellId, x, y, seatSize);

                // Record its center point
                int centerX = x + seatSize / 2;
                int centerY = y + seatSize / 2;
                seatCenterMap.put(cellId, new Point(centerX, centerY));
            }
            case TOILET -> {
                // Light‐pink toilet box
                g.setColor(new Color(255, 230, 230));
                g.fillRect(x, y, toiletSize, toiletSize);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, toiletSize, toiletSize);

                // “Toilet” label
                Font tFont = new Font("SansSerif", Font.PLAIN, 14);
                g.setFont(tFont);
                FontMetrics tfm = g.getFontMetrics();
                String label = "Toilet";
                int textX = x + (toiletSize - tfm.stringWidth(label)) / 2;
                int textY = y + (toiletSize + tfm.getAscent()) / 2 - 4;
                g.drawString(label, textX, textY);
            }
            case EMPTY -> {
                // Light‐gray empty placeholder
                g.setColor(new Color(245, 245, 245));
                g.fillRect(x, y, seatSize, seatSize);
                g.setColor(Color.GRAY);
                g.drawRect(x, y, seatSize, seatSize);
            }
            default -> {
                // no-op; should never hit
            }
        }
    }

    /** Helper to draw a seat label (e.g. “C4”) centered within a seat cell. */
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

    /**
     * Simple value‐object to hold the result of generation:
     *   ● A BufferedImage of the full bus layout
     *   ● A Map of seatID → center‐Point
     */
    public record GenerationResult (
        BufferedImage image,
        Map<String, Point> seatCenters
    ){}
}
