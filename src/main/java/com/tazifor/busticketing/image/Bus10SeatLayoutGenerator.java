package com.tazifor.busticketing.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class Bus10SeatLayoutGenerator {
    public static void main(String[] args) throws Exception {
        // ────── CONFIGURATION ──────
        int rows = 4;                // total rows of seat/groups (3 seat‐rows + 1 row for toilet/empty)
        int seatSize = 60;           // each seat is 60×60 px
        int seatGap = 10;            // gap between seats
        int aisleWidth = 40;         // width of the central aisle
        int padding = 20;            // padding around edges
        int driverAreaHeight = 80;   // height for driver area at top-left
        int labelHeight = 40;        // space for "Driver" and "Entrance" labels

        // Calculate image dimensions
        int width = padding * 2                   // left + right padding
            + seatSize * 2                   // two seats on left side
            + seatGap                         // gap between left seats & aisle
            + aisleWidth                     // central aisle
            + seatGap                         // gap between aisle & right area
            + seatSize;                      // right area (seat or toilet or empty)

        int height = padding                       // top padding
            + labelHeight                   // "Driver"/"Entrance" text area
            + driverAreaHeight             // driver box
            + seatGap                       // gap below driver area
            + rows * seatSize              // seats (or toilet/empty) stacked vertically
            + (rows - 1) * seatGap         // gaps between each row
            + padding;                     // bottom padding

        // ────── CREATE A BLANK IMAGE ──────
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            // Antialias for text
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 1) Fill background white
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            // 2) Draw "Driver" area at top-left
            //    a) Draw a light-blue rectangle
            g.setColor(new Color(200, 230, 255));
            int driverY = padding;
            int driverX = padding;
            int driverW = seatSize;      // same width as one seat
            int driverH = driverAreaHeight;
            g.fillRect(driverX, driverY, driverW, driverH);
            //    b) Outline and label "Driver"
            g.setColor(Color.BLACK);
            g.drawRect(driverX, driverY, driverW, driverH);
            Font driverFont = new Font("SansSerif", Font.BOLD, 16);
            g.setFont(driverFont);
            FontMetrics dfm = g.getFontMetrics();
            String driverLabel = "Driver";
            int dLabelX = driverX + (driverW - dfm.stringWidth(driverLabel)) / 2;
            int dLabelY = driverY + (driverH + dfm.getAscent()) / 2 - 4;
            g.drawString(driverLabel, dLabelX, dLabelY);

            // 3) Draw "Entrance" area at top-right
            //    a) Light-green rectangle
            int entranceX = padding + seatSize * 2 + seatGap + aisleWidth + seatGap;
            int entranceY = padding;
            int entranceW = seatSize;
            int entranceH = driverAreaHeight;
            g.setColor(new Color(200, 255, 200));
            g.fillRect(entranceX, entranceY, entranceW, entranceH);
            //    b) Outline and label "Entrance"
            g.setColor(Color.BLACK);
            g.drawRect(entranceX, entranceY, entranceW, entranceH);
            Font entranceFont = new Font("SansSerif", Font.BOLD, 16);
            g.setFont(entranceFont);
            FontMetrics efm = g.getFontMetrics();
            String entranceLabel = "Entrance";
            int eLabelX = entranceX + (entranceW - efm.stringWidth(entranceLabel)) / 2;
            int eLabelY = entranceY + (entranceH + efm.getAscent()) / 2 - 4;
            g.drawString(entranceLabel, eLabelX, eLabelY);

            // 4) Draw the aisle below the driver/entrance
            int aisleX = padding + seatSize * 2 + seatGap;
            int aisleY = padding + driverAreaHeight + seatGap;
            int aisleH = height - aisleY - padding;
            g.setColor(new Color(230, 230, 230)); // light gray
            g.fillRect(aisleX, aisleY, aisleWidth, aisleH);

            // 5) Draw seats and toilet for each row
            int startY = aisleY;
            for (int r = 0; r < rows; r++) {
                int y = startY + r * (seatSize + seatGap);

                // Row 1 and 2: seats on left and right
                // Row 3: seats on left, toilet on right
                // Row 4: seats on left, empty on right

                // ─── Left side seats (2 seats per row) ───
                // Leftmost seat (Column A)
                int colAX = padding;
                g.setColor(Color.WHITE);
                g.fillRect(colAX, y, seatSize, seatSize);
                g.setColor(Color.BLACK);
                g.drawRect(colAX, y, seatSize, seatSize);
                drawSeatLabel(g, "A" + (r + 1), colAX, y, seatSize);

                // Next left seat  (Column B)
                int colBX = padding + seatSize + seatGap;
                g.setColor(Color.WHITE);
                g.fillRect(colBX, y, seatSize, seatSize);
                g.setColor(Color.BLACK);
                g.drawRect(colBX, y, seatSize, seatSize);
                drawSeatLabel(g, "B" + (r + 1), colBX, y, seatSize);

                // ─── Right side area ───
                int rightX = aisleX + aisleWidth + seatGap;
                if (r == 0 || r == 1) {
                    // Row 1 & 2: one seat on right (Column C)
                    g.setColor(Color.WHITE);
                    g.fillRect(rightX, y, seatSize, seatSize);
                    g.setColor(Color.BLACK);
                    g.drawRect(rightX, y, seatSize, seatSize);
                    drawSeatLabel(g, "C" + (r + 1), rightX, y, seatSize);
                } else if (r == 2) {
                    // Row 3: draw toilet box
                    g.setColor(new Color(255, 230, 230));
                    g.fillRect(rightX, y, seatSize, seatSize);
                    g.setColor(Color.BLACK);
                    g.drawRect(rightX, y, seatSize, seatSize);
                    Font toiletFont = new Font("SansSerif", Font.PLAIN, 14);
                    g.setFont(toiletFont);
                    FontMetrics tfm = g.getFontMetrics();
                    String toiletLabel = "Toilet";
                    int tLabelX = rightX + (seatSize - tfm.stringWidth(toiletLabel)) / 2;
                    int tLabelY = y + (seatSize + tfm.getAscent()) / 2 - 4;
                    g.drawString(toiletLabel, tLabelX, tLabelY);
                } else {
                    // Row 4: no seat (empty space)
                    // We can draw a dashed outline or leave blank; here we just leave it blank
                    g.setColor(new Color(245, 245, 245));
                    g.fillRect(rightX, y, seatSize, seatSize);
                    g.setColor(Color.GRAY);
                    g.drawRect(rightX, y, seatSize, seatSize);
                }
            }
        } finally {
            g.dispose();
        }

        // ────── SAVE PNG TO DISK ──────
        String outputFilename = "bus_10_seats.png";
        ImageIO.write(img, "PNG", new File(outputFilename));
        System.out.println("10‐seat bus layout generated: " + new File(outputFilename).getAbsolutePath());

        // ────── OPTIONAL: PRINT BASE64   ──────
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        System.out.println("\nPaste this under \"base64\" in your Flow JSON:\n");
        System.out.println("data:image/png;base64," + b64);
    }

    /**
     * Helper to draw a seat label (e.g. "A3") centered inside a seat rectangle.
     */
    private static void drawSeatLabel(Graphics2D g, String label, int x, int y, int size) {
        Font seatFont = new Font("SansSerif", Font.BOLD, 14);
        g.setFont(seatFont);
        FontMetrics fm = g.getFontMetrics(seatFont);
        int textWidth = fm.stringWidth(label);
        int textX = x + (size - textWidth) / 2;
        int textY = y + (size + fm.getAscent()) / 2 - 4;
        g.setColor(Color.BLACK);
        g.drawString(label, textX, textY);
    }
}

