package com.nourri.busticketing;

import com.nourri.busticketing.config.properties.BusLayoutProperties;
import com.nourri.busticketing.config.properties.EncryptionConfig;
import com.nourri.busticketing.config.properties.FontProperties;
import com.nourri.busticketing.config.properties.WhatsAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
@EnableConfigurationProperties({
    WhatsAppProperties.class,
    EncryptionConfig.class,
    BusLayoutProperties.class,
    FontProperties.class
})
public class BusTicketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusTicketingApplication.class, args);
    }

}
