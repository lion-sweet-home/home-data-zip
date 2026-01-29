package org.example.homedatazip.busstation.client;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class SeoulBusOpenApiClient {

    @Value("${seoul.openapi.base-url}")
    private String baseUrl;

    @Value("${seoul.openapi.key}")
    private String key;

    @Value("${seoul.openapi.service}")
    private String service;

    @Value("${seoul.openapi.type}")
    private String type;

    private WebClient webClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public SeoulBusStopResponse fetch(int startIndex, int endIndex) {
        return webClient().get()
                .uri("/{key}/{type}/{service}/{start}/{end}/",
                        key, type, service, startIndex, endIndex)
                .retrieve()
                .bodyToMono(SeoulBusStopResponse.class)
                .block();
    }
}
