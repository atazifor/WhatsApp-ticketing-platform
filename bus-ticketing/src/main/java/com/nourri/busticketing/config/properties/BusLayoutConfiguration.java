package com.nourri.busticketing.config.properties;

import com.nourri.busticketing.LayoutAssets;
import com.nourri.busticketing.config.BusLayoutConfig;
import com.nourri.busticketing.config.FontConfig;
import com.nourri.busticketing.generator.BusLayoutGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusLayoutConfiguration {

    @Bean
    public FontConfig fontConfig(BusConfigMapper mapper, FontProperties props) {
        return mapper.toDomainConfig(props);
    }

    @Bean
    public BusLayoutConfig busLayoutConfig(BusConfigMapper mapper, BusLayoutProperties props) {
        return mapper.toDomainConfig(props);
    }

    @Bean
    public LayoutAssets layoutAssets(BusLayoutConfig layoutConfig, FontConfig fontConfig) {
        BusLayoutGenerator generator = new BusLayoutGenerator(layoutConfig, fontConfig);
        return generator.generateLayout();
    }
}

