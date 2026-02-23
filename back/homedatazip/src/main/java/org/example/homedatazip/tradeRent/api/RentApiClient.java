package org.example.homedatazip.tradeRent.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.dto.ApiResponse;
import org.example.homedatazip.tradeRent.dto.RentApiItem;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentApiClient {

    private final RentApiProperties props;
    private final WebClient webClient;
    private final XmlMapper xmlMapper = XmlMapper.builder()
            .addModule(new ParameterNamesModule())
            .build();

    public ApiResponse<RentApiItem> fetch(String sggCd5, String dealYmd6, int page) {

        String uri = UriComponentsBuilder.fromUriString(props.getBaseUrl() + props.getPath())
                .queryParam("LAWD_CD", sggCd5)
                .queryParam("DEAL_YMD", dealYmd6)
                .queryParam("serviceKey", props.getServiceKey())
                .queryParam("pageNo", page)
                .queryParam("numOfRows", props.getNumOfRows())
                .build(false)
                .toUriString();

        log.info("========== Rent API 호출 ==========");
        log.info("URL: {}", uri.replaceAll("serviceKey=[^&]+", "serviceKey=***HIDDEN***"));

        String xml = webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .doOnError(e -> log.error("WebClient 에러: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("API 호출 실패 - 에러 타입: {}, 메시지: {}", e.getClass().getSimpleName(), e.getMessage());
                    return Mono.empty();
                })
                .block();

        log.info("XML 응답 길이: {}", xml == null ? "null" : xml.length());
        if (xml != null && xml.length() < 500) {
            log.info("XML 응답 내용: {}", xml);
        }

        if (xml == null || xml.isBlank()) return null;

        try {
            ApiResponse<RentApiItem> res =
                    xmlMapper.readValue(xml, new TypeReference<ApiResponse<RentApiItem>>() {});
            log.info("파싱 성공 - resultCode: {}, totalCount: {}",
                    res.header() != null ? res.header().resultCode() : "N/A",
                    res.body() != null ? res.body().totalCount() : "N/A");
            return res;
        } catch (Exception e) {
            log.error("XML 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
