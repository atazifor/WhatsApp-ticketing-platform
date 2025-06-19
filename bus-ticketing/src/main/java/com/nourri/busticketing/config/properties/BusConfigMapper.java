package com.nourri.busticketing.config.properties;

import com.nourri.busticketing.config.BusLayoutConfig;
import com.nourri.busticketing.config.FontConfig;
import org.springframework.stereotype.Component;

@Component
public class BusConfigMapper {
    // Convert BusLayoutProperties → BusLayoutConfig
    public BusLayoutConfig toDomainConfig(BusLayoutProperties props) {
        return new BusLayoutConfig(
            props.rows(),
            props.colsLeft(),
            props.colsRight(),
            props.seatSize(),
            props.seatGap(),
            props.aisleWidth(),
            props.padding(),
            props.driverAreaHeight(),
            props.labelHeight(),
            props.seatStyle(),
            props.layoutMap()
        );
    }

    // Convert FontProperties → FontConfig
    public FontConfig toDomainConfig(FontProperties props) {
        return new FontConfig(
            props.seatLabelSize(),
            props.driverLabelSize(),
            props.toiletLabelSize(),
            props.legendLabelSize()
        );
    }
}
