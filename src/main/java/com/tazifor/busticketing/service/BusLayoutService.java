package com.tazifor.busticketing.service;

import com.tazifor.busticketing.config.properties.BusLayoutConfig;
import com.tazifor.busticketing.model.BusLayoutCellType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BusLayoutService
 *
 * 1) Reads BusLayoutConfig at startup.
 * 2) Builds a BufferedImage of the entire bus layout, row by row, cell by cell.
 * 3) Computes a Map<String,Point> of every SEAT’s center coordinates (e.g. "A1"→Point(x,y)).
 * 4) Caches the Base64-encoded PNG as a “base64,…” string.
 */
@Service
public class BusLayoutService {
    private static final Logger log = LoggerFactory.getLogger(BusLayoutService.class);

    private final BusLayoutConfig cfg;

    /** After initialize(), this holds each seat’s center: "A1"→Point(x,y), "B3"→Point(x,y), etc.
     * -- GETTER --
     * Returns the cached map of seatID → center Point(x,y).
     */
    @Getter
    private Map<String, Point> seatCoordinates;

    /** After initialize(), this is the full bus image as a Base64 data URI.
     * -- GETTER --
     * Returns the cached “base64,…” string of the bus layout PNG.
     */
    @Getter
    private String base64BusImage;

    public BusLayoutService(BusLayoutConfig cfg) {
        this.cfg = cfg;
    }

    @PostConstruct
    public void initialize() {
        try {
            // ─── 1) Read all parameters from cfg ──────────────────────────────
            int rows = cfg.rows();                     // e.g. 5
            int colsLeft = cfg.colsLeft();             // e.g. 2
            int colsRight = cfg.colsRight();           // e.g. 1
            int seatSize = cfg.seatSize();             // e.g. 60
            int seatGap = cfg.seatGap();               // e.g. 10
            int aisleWidth = cfg.aisleWidth();         // e.g. 40
            int padding = cfg.padding();               // e.g. 20
            int driverH = cfg.driverAreaHeight();      // e.g. 80
            int labelH = cfg.labelHeight();            // e.g. 40

            List<List<BusLayoutCellType>> layoutMap = cfg.layoutMap();
            if (layoutMap == null || layoutMap.size() != rows) {
                throw new IllegalStateException(
                    "layoutMap must have exactly " + rows + " rows"
                );
            }
            for (List<BusLayoutCellType> row : layoutMap) {
                if (row.size() != colsLeft + colsRight) {
                    throw new IllegalStateException(
                        "Each row in layoutMap must have length = colsLeft + colsRight = "
                            + (colsLeft + colsRight)
                    );
                }
            }

            // ─── 2) Compute the overall image dimensions ───────────────────────
            // Left block width = (colsLeft * seatSize) + ((colsLeft - 1) * seatGap)
            int leftBlockWidth = colsLeft * seatSize + (colsLeft - 1) * seatGap;
            // Right block width = (colsRight * seatSize) + ((colsRight - 1) * seatGap)
            int rightBlockWidth = colsRight * seatSize + (colsRight - 1) * seatGap;
            // Total width = padding*2 + leftBlock + seatGap + aisle + seatGap + rightBlock
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

            log.info("Generating bus layout image of size {}×{} (rows={}, colsLeft={}, colsRight={})",
                width, height, rows, colsLeft, colsRight);

            // ─── 3) Create a blank BufferedImage and Graphics2D ────────────────
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                // 3a) Enable antialiased text
                g.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                );

                // 3b) Fill the background white
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, width, height);

                // 3c) Draw “Driver” and “Entrance” special cells from row 0
                // Row 0’s vertical offset = padding
                int driverRowY = padding;

                // Draw each column in row 0 (two left cells + one right cell)
                //   - Column 0 (c=0) maps to x = padding + 0*(seatSize + seatGap)
                //   - Column 1 (c=1) maps to x = padding + 1*(seatSize + seatGap)
                //   - Column 2 (c=2) maps to x = padding + leftBlockWidth + seatGap + aisleWidth + seatGap + (0*(seatSize + seatGap))
                // We’ll call drawCell for rowIndex=0, colIndex=0..2

                // But before we can draw seats, we must initialize seatCoordinates
                seatCoordinates = new LinkedHashMap<>();

                // 3d) Now iterate every row r=0..rows-1, every column c=0..(colsLeft+colsRight-1)
                for (int r = 0; r < rows; r++) {
                    // y top of this row’s cell group:
                    int rowY = padding + labelH + (r == 0 ? 0 : (driverH + seatGap))
                        + (r > 0 ? ((r - 1) * (seatSize + seatGap)) : 0);

                    // That formula resolves to:
                    //  - If r=0: y = padding (the driver row)
                    //  - If r>0: y = padding + labelH + driverH + seatGap + (r-1)*(seatSize+seatGap)

                    for (int c = 0; c < colsLeft + colsRight; c++) {
                        // Compute the x-coordinate of this cell:
                        int x;
                        if (c < colsLeft) {
                            // one of the left seats
                            x = padding + c * (seatSize + seatGap);
                        } else {
                            // one of the right seats (to the right of the aisle)
                            int rightIndex = c - colsLeft; // 0..(colsRight−1)
                            x = padding
                                + leftBlockWidth
                                + seatGap
                                + aisleWidth
                                + seatGap
                                + rightIndex * (seatSize + seatGap);
                        }

                        // Get the CellType from the layoutMap
                        BusLayoutCellType type = layoutMap.get(r).get(c);
                        drawCell(
                            g,
                            type,
                            r,
                            c,
                            x,
                            rowY,
                            seatSize,
                            seatSize,
                            seatCoordinates
                        );
                    }
                }
            } finally {
                g.dispose();
            }

            // ─── 4) Encode the final BufferedImage to Base64 ─────────────────────
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(img, "PNG", baos);
                base64BusImage = Base64.getEncoder().encodeToString(baos.toByteArray());
            }

            log.info("BusLayoutService: cached base64 image and {} seat coordinates",
                seatCoordinates.size());

        } catch (Exception ex) {
            throw new RuntimeException("BusLayoutService initialization failed", ex);
        }
    }

    /**
     * Draws exactly one cell of the bus layout at (x,y) with size (w×h), given its type.
     * Populates seatCoordinates if BusLayoutCellType == SEAT.
     *
     * @param g               Graphics context
     * @param type            one of DRIVER, ENTRANCE, SEAT, TOILET, EMPTY
     * @param rowIndex        0-based row (0 → row “0”/Driver row, 1 → first seat row, etc.)
     * @param colIndex        0-based column (0 → leftmost cell, … up to colsLeft+colsRight-1)
     * @param x               top-left X of this cell
     * @param y               top-left Y of this cell
     * @param w               cell width = seatSize
     * @param h               cell height = seatSize (for seats/toilets/empty) or driverAreaHeight (for row 0)
     * @param seatCoordinates map we populate for each SEAT cell
     */
    private void drawCell(Graphics2D g,
                          BusLayoutCellType type,
                          int rowIndex, int colIndex,
                          int x, int y, int w, int h,
                          Map<String, Point> seatCoordinates) {
        // Seat ID is rowLetter + columnNumber (1-based in that row)
        // For row 0, we’ll still call it "A1","B1","C1" even though they’re driver/empty/entrance
        String rowLetter = String.valueOf((char) ('A' + rowIndex));
        String cellId = rowLetter + (colIndex + 1);

        switch (type) {
            case DRIVER:
                // Light-blue driver box
                g.setColor(new Color(200, 230, 255));
                g.fillRect(x, y, w, h);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, w, h);
                // Label "Driver" centered
                Font driverFont = new Font("SansSerif", Font.BOLD, 16);
                g.setFont(driverFont);
                FontMetrics dfm = g.getFontMetrics();
                String driverLabel = "Driver";
                int dLabelX = x + (w - dfm.stringWidth(driverLabel)) / 2;
                int dLabelY = y + (h + dfm.getAscent()) / 2 - 4;
                g.drawString(driverLabel, dLabelX, dLabelY);
                break;

            case ENTRANCE:
                // Light-green entrance box
                g.setColor(new Color(200, 255, 200));
                g.fillRect(x, y, w, h);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, w, h);
                // Label "Entrance" centered
                Font entranceFont = new Font("SansSerif", Font.BOLD, 16);
                g.setFont(entranceFont);
                FontMetrics efm = g.getFontMetrics();
                String entranceLabel = "Entrance";
                int eLabelX = x + (w - efm.stringWidth(entranceLabel)) / 2;
                int eLabelY = y + (h + efm.getAscent()) / 2 - 4;
                g.drawString(entranceLabel, eLabelX, eLabelY);
                break;

            case SEAT:
                // White seat with black border
                g.setColor(Color.WHITE);
                g.fillRect(x, y, w, h);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, w, h);
                // Label the seat (e.g. "B3") centered
                drawSeatLabel(g, cellId, x, y, w);
                // Store its center point
                int centerX = x + w / 2;
                int centerY = y + h / 2;
                seatCoordinates.put(cellId, new Point(centerX, centerY));
                break;

            case TOILET:
                // Light-pink toilet cell
                g.setColor(new Color(255, 230, 230));
                g.fillRect(x, y, w, h);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, w, h);
                // “Toilet” label
                Font toiletFont = new Font("SansSerif", Font.PLAIN, 14);
                g.setFont(toiletFont);
                FontMetrics tfm = g.getFontMetrics();
                String toiletLabel = "Toilet";
                int tLabelX = x + (w - tfm.stringWidth(toiletLabel)) / 2;
                int tLabelY = y + (h + tfm.getAscent()) / 2 - 4;
                g.drawString(toiletLabel, tLabelX, tLabelY);
                break;

            case EMPTY:
                // Light gray empty placeholder
                g.setColor(new Color(245, 245, 245));
                g.fillRect(x, y, w, h);
                g.setColor(Color.GRAY);
                g.drawRect(x, y, w, h);
                break;

            default:
                // Should never happen if your YAML uses only valid BusLayoutCellType strings
                break;
        }
    }

    /** Draws a seat label (e.g. "A3") centered inside a seat cell at (x,y) of size w×w. */
    private void drawSeatLabel(Graphics2D g, String label, int x, int y, int w) {
        Font seatFont = new Font("SansSerif", Font.BOLD, 14);
        g.setFont(seatFont);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textX = x + (w - textWidth) / 2;
        int textY = y + (w + fm.getAscent()) / 2 - 4;
        g.setColor(Color.BLACK);
        g.drawString(label, textX, textY);
    }

}
