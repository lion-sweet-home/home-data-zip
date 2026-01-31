package org.example.homedatazip.global.batch.hospital.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.hospital.dto.HospitalApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Open API 호출 클라이언트
 * <br/>
 * API 호출 URL 형식
 * https://apis.data.go.kr/B552657/HsptlAsembySearchService/getHsptlMdcncFullDown?serviceKey={serviceKey}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HospitalApiClient {

    private final WebClient webClient;

    @Value("${api.seoul.hospital.key}")
    private String apiKey;

    @Value("${api.data.hospital.key}")
    private String serviceKey;

    private static final String BASE_URL
//            = "http://apis.data.go.kr/B552657/HsptlAsembySearchService";
            = "http://openapi.seoul.go.kr:8088";
    private static final String TYPE = "json";
    private static final String SERVICE_NAME
//            = "getHsptlMdcncFullDown";
            = "TbHospitalInfo";

    public HospitalApiResponse fetchHospitals(
//            int pageNo,
//            int numOfRows
            int startIndex,
            int endIndex
    ) {
        String url = String.format("%s/%s/%s/%s/%d/%d",
                BASE_URL,
                apiKey,
                TYPE,
                SERVICE_NAME,
                startIndex,
                endIndex);

        log.info("API 호출: {} ~ {}", startIndex, endIndex);
//        log.info("API 호출: pageNo={}, numOfRows={}", pageNo, numOfRows);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(HospitalApiResponse.class)
                .block();
    }

    /**
     * 전체 데이터 건수 조회
     */
    public Integer getTotalCount() {
        HospitalApiResponse response = fetchHospitals(1, 1);
        return response.getListTotalCount();
    }
}
