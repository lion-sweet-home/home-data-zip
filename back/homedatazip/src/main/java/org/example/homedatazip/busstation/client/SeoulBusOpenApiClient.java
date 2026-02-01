package org.example.homedatazip.busstation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
                // 여기서 JSON이 아니면(=XML) UnsupportedMediaType 터질 수 있어서 String으로 받아서 체크
                .bodyToMono(String.class)
                .map(body -> {
                    // XML(인증키 오류 등) 방어
                    if (body != null && body.startsWith("<")) {
                        throw new IllegalStateException("Expected JSON but got XML. body=" + body);
                    }
                    return body;
                })
                .flatMap(body -> webClient().mutate().build()
                        .post() // Jackson 파싱만 하려고 임시로 쓰는 꼼수 싫으면 ObjectMapper 직접 써도 됨
                        .uri("http://localhost/") // 실제 호출 안 함(사용 안 됨)
                        .exchangeToMono(r -> Mono.just(body))
                )
                // 사실상 위 플랫맵 필요 없음. 깔끔하게 ObjectMapper로 가는 버전이 더 낫다.
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
