package org.example.homedatazip.tradeRent.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "rent-api")
public class RentApiProperties {
    private String baseUrl;
    private String path;
    private String serviceKey;
    private int numOfRows;
    private int timeoutSeconds;
}
