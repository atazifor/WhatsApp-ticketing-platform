package com.nourri.busticketing.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bus.layout.font")
public record FontProperties(
    int seatLabelSize,
    int driverLabelSize,
    int toiletLabelSize,
    int legendLabelSize
) {}
