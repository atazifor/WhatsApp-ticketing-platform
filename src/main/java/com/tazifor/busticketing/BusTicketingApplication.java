package com.tazifor.busticketing;

import com.tazifor.busticketing.config.properties.BusLayoutConfig;
import com.tazifor.busticketing.config.properties.EncryptionConfig;
import com.tazifor.busticketing.config.properties.WhatsAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    WhatsAppProperties.class,
    EncryptionConfig.class,
    BusLayoutConfig.class
})
public class BusTicketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusTicketingApplication.class, args);
    }

}
