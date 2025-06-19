package com.nourri.busticketing.config;

import java.awt.*;

public record FontConfig(
    int seatLabelSize,
    int driverLabelSize,
    int toiletLabelSize,
    int legendLabelSize
) {
    public Font seatFont() {
        return new Font("SansSerif", Font.BOLD, seatLabelSize);
    }

    public Font driverFont() {
        return new Font("SansSerif", Font.BOLD, driverLabelSize);
    }

    public Font toiletFont() {
        return new Font("SansSerif", Font.PLAIN, toiletLabelSize);
    }

    public Font legendFont() {
        return new Font("SansSerif", Font.PLAIN, legendLabelSize);
    }
}