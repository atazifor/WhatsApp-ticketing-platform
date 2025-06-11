package com.tazifor.busticketing.util.encoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tazifor.busticketing.model.BookingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class BookingStateCodec {
    private static final Logger logger = LoggerFactory.getLogger(BookingStateCodec.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String encode(BookingState state) {
        try {
            String json = mapper.writeValueAsString(state);
            return Base64.getEncoder().encodeToString(json.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode BookingState", e);
        }
    }

    public static BookingState decode(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded);
            return mapper.readValue(json, BookingState.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode BookingState", e);
        }
    }
}