package org.example.homedatazip.global.config;

import org.example.homedatazip.global.batch.tradeRent.properties.TradeRentBatchProperties;
import org.example.homedatazip.tradeRent.api.RentApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        CorsProperties.class,
        RentApiProperties.class,
        TradeRentBatchProperties.class
})
public class PropertiesConfig {}
