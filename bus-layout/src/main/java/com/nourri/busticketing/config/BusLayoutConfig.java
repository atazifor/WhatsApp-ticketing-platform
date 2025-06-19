package com.nourri.busticketing.config;

import com.nourri.busticketing.BusLayoutCellType;

import java.util.List;

/**
 * Binds to application.yaml’s bus.layout.* properties.
 */
public record BusLayoutConfig (
    // ─── “rows” = number of vertical rows
    int rows,

    // ─── seats on the left in a full row
    int colsLeft,

    // ─── seats on the right in a full row
    int colsRight,

    // ─── pixel sizes
    int seatSize,
    int seatGap,
    int aisleWidth,
    int padding,
    int driverAreaHeight,
    int labelHeight,
    String seatStyle,

    /**
     * A 2D list of length = rows. Each inner list must have length = colsLeft + colsRight.
     * Each element must be one of: DRIVER, ENTRANCE, SEAT, TOILET, EMPTY.
     */
    List<List<BusLayoutCellType>> layoutMap
) {}
