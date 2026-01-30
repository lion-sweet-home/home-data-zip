package org.example.homedatazip.global.batch.tradeRent.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class TradeRentBackfillProperties {
    private boolean enabled = false;

}
