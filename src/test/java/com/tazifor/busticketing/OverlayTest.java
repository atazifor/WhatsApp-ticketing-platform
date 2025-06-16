package com.tazifor.busticketing;

import com.tazifor.busticketing.util.ImageOverlayUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Set;

@SpringBootTest
public class OverlayTest {
    @Autowired
    ImageOverlayUtil util;


    @Test
    public void testOverlay() throws IOException {
        // 2. Generate availability overlay (pass some sample available seats)
        Set<String> available = Set.of("1", "2", "5");
        String b64 = util.createAvailabilityOverlay(available);

        // 3. Strip "data:image/png;base64," and decode
        byte[] imgBytes = Base64.getDecoder().decode(b64);

        // 4. Save PNG to disk
        Path out = Paths.get("seat_availability_test.png");
        Files.write(out, imgBytes);
        System.out.println("Wrote test image to: " + out.toAbsolutePath());

    }
}
