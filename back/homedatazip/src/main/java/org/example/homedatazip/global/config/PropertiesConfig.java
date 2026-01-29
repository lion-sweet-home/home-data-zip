package org.example.homedatazip.global.config;

import org.example.homedatazip.tradeRent.api.MolitRentProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        CorsProperties.class,
        MolitRentProperties.class
})
public class PropertiesConfig {}
