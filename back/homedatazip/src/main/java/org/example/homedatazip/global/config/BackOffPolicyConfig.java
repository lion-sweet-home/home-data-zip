package org.example.homedatazip.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;

@Configuration
public class BackOffPolicyConfig {

    @Value("${batch.backoff.external-api:2000}")
    private long externalApiBackOff;

    @Bean
    public FixedBackOffPolicy externalApiBackOffPolicy() {
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(externalApiBackOff);
        return policy;
    }
}
