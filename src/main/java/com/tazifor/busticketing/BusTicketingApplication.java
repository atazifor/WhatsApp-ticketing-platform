package com.tazifor.busticketing;

import com.tazifor.busticketing.config.properties.BusLayoutConfig;
import com.tazifor.busticketing.config.properties.EncryptionConfig;
import com.tazifor.busticketing.config.properties.FontConfig;
import com.tazifor.busticketing.config.properties.WhatsAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
@EnableConfigurationProperties({
    WhatsAppProperties.class,
    EncryptionConfig.class,
    BusLayoutConfig.class,
    FontConfig.class
})
public class BusTicketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusTicketingApplication.class, args);
    }

}
