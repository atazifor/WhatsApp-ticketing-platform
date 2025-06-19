package com.nourri.busticketing;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/** Holds the generated image and lookup maps */
public record LayoutAssets(
    BufferedImage image,
    Map<String, Point> seatCenters,
    Map<String, Rectangle> seatBounds
) {
    public String getBaseImageAsBase64() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
