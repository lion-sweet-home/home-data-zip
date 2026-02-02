package org.example.homedatazip.global.batch.hospital.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.hospital.dto.HospitalApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * Open API 호출 클라이언트
 * <br/>
 * API 호출 URL 형식
 * http://apis.data.go.kr/B552657/HsptlAsembySearchService/getHsptlMdcncFullDown?serviceKey={serviceKey}&pageNo={pageNo}&numOfRows={numOfRows}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HospitalApiClient {

    private final WebClient webClient;
    private final XmlMapper xmlMapper = new XmlMapper();

    @Value("${api.data.hospital.key}")
    private String serviceKey;

    private static final String BASE_URL
            = "http://apis.data.go.kr/B552657/HsptlAsembySearchService";
    private static final String SERVICE_NAME = "getHsptlMdcncFullDown";

    private static final int API_CALL_DELAY_MS = 100; // Rate Limiting 방지

    public HospitalApiResponse fetchHospital(
            int pageNo,
            int numOfRows
    ) {
        String url = String.format("%s/%s?serviceKey=%s&pageNo=%d&numOfRows=%d",
                BASE_URL,
                SERVICE_NAME,
                serviceKey,
                pageNo,
                numOfRows
        );

        log.info("API 호출: pageNo={}, numOfRows={}", pageNo, numOfRows);

        try {
            Thread.sleep(API_CALL_DELAY_MS);

            String xmlResponse = webClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30)) // 최대 대기시간
                    .block();

            return xmlMapper.readValue(xmlResponse, HospitalApiResponse.class);
        } catch (Exception e) {
            log.error("API 호출 또는 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("Hospital API 호출 실패", e);
        }
    }

    public Integer getTotalCount() {
        HospitalApiResponse response = fetchHospital(1, 1);
        return response.getTotalCount();
    }
}
