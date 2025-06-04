package com.tazifor.busticketing.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


/**
 * Loads configured images (PNG/JPG) from classpath or filesystem,
 * Base64-encodes them once at startup, and caches the "data:image/…;base64,…" string.
 */
@Component
public class ImageBase64Cache {
    private static final Logger log = LoggerFactory.getLogger(ImageBase64Cache.class);

    /**
     * Configure which images to cache. In application.yaml, set:
     *
     * images.toCache[0]=classpath:static/bus_10_seats.png
     */
    @Value("${seating.base-images[0]}")
    private Resource[] imageResources;

    /** Map from logical name (filename without extension) → Base64 data URI */
    private final Map<String, String> base64Map = new HashMap<>();

    @PostConstruct
    public void initialize() {
        for (Resource res : imageResources) {
            try {
                String filename = res.getFilename();
                if (filename == null) {
                    log.warn("Skipping resource with no filename: {}", res);
                    continue;
                }
                // Key = filename without extension, e.g. "bus_10_seats"
                String key = filename.replaceAll("\\.[^.]+$", "");

                BufferedImage img = ImageIO.read(res.getInputStream());
                if (img == null) {
                    log.warn("Could not read image {}; skipping.", filename);
                    continue;
                }

                // Encode to PNG regardless of extension
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "PNG", baos);
                String b64DataUri = Base64.getEncoder().encodeToString(baos.toByteArray());

                base64Map.put(key, b64DataUri);
                log.info("Cached Base64 image for key='{}'", key);
            } catch (Exception e) {
                log.error("Failed to load/encode image resource {}: {}", res, e.getMessage());
            }
        }
    }

    /** Returns the cached Base64 Data URI (or null if not found). */
    public String getBase64(String key) {
        return base64Map.get(key);
    }

}
