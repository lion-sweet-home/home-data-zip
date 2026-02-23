package org.example.homedatazip.busstation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

//서울 API 응답 JSON 모양을 그대로 담는 그릇(DTO)

@Component
@RequiredArgsConstructor
@Slf4j
public class SeoulBusOpenApiClient {

    @Value("${seoul.openapi.base-url}")
    private String baseUrl;

    @Value("${seoul.openapi.key}")
    private String key;

    @Value("${seoul.openapi.service}")
    private String service;

    @Value("${seoul.openapi.type}")
    private String type; // json

    private final WebClient.Builder builder;

    private WebClient webClient() {
        return builder.baseUrl(baseUrl).build();
    }

    public SeoulBusStopResponse fetch(int startIndex, int endIndex) {

        return webClient().get()
                .uri("/{key}/{type}/{service}/{start}/{end}/",
                        key, type, service, startIndex, endIndex)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        resp -> resp.bodyToMono(String.class)
                                .doOnNext(body -> log.error("[SEOUL API] error body={}", body))
                                .flatMap(body -> Mono.error(new IllegalStateException("서울 API 실패: " + body)))
                )

                .bodyToMono(String.class)
                .map(body -> {
                    // XML(인증키 오류 등) 방어
                    if (body != null && body.startsWith("<")) {
                        throw new IllegalStateException("Expected JSON but got XML. body=" + body);
                    }
                    return body;
                })
                .flatMap(body -> webClient().mutate().build()
                        .post()
                        .uri("http://localhost/")
                        .exchangeToMono(r -> Mono.just(body))
                )

                .map(body -> {
                    try {
                        // ObjectMapper 직접 사용
                        return new com.fasterxml.jackson.databind.ObjectMapper().readValue(body, SeoulBusStopResponse.class);
                    } catch (Exception e) {
                        throw new IllegalStateException("JSON parse fail. body=" + body, e);
                    }
                })
                .block();
    }
}
