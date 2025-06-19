package com.nourri.busticketing.config.properties;

import com.nourri.busticketing.BusLayoutCellType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bus.layout")
public record BusLayoutProperties(
    int rows,
    int colsLeft,
    int colsRight,
    int seatSize,
    int seatGap,
    int aisleWidth,
    int padding,
    int driverAreaHeight,
    int labelHeight,
    String seatStyle,
    List<List<BusLayoutCellType>> layoutMap
) {}
