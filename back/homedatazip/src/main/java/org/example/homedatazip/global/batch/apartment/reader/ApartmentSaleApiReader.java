package org.example.homedatazip.global.batch.apartment.reader;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.dto.ApiResponse;
import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.ArrayList;
import java.util.List;

@StepScope
@Component
@Slf4j
public class ApartmentSaleApiReader implements ItemReader<ApartmentTradeSaleItem> {


    private final WebClient webClient;

    @Value("${api.data-go-kr.service-key}")
    private String serviceKey;

    @Value("#{stepExecutionContext['lawdCd']}")
    String lawdCd;

    @Value("#{stepExecutionContext['dealYmd']}")
    String dealYmd;

    private List<ApartmentTradeSaleItem> items = new ArrayList<>();
    private int nextItemIndex = 0;
    private int currentPage = 1;
    private int totalCount = -1;

    private final XmlMapper xmlMapper = new XmlMapper();


    public ApartmentSaleApiReader(WebClient.Builder builder) {
        // 1. 공공데이터 전용 인코딩 방지 설정
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        // baseUrl을 설정하지 않고 builder만 완성합니다.
        this.webClient = builder
                .uriBuilderFactory(factory)
                .build();
    }

    @Override
    public ApartmentTradeSaleItem read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (totalCount == -1 || (nextItemIndex >= items.size() && currentPage * 100 < totalCount)) {
            log.info(">>> API 호출 시도: 지역코드={}, 날짜={}", lawdCd, dealYmd);
            if (totalCount != -1) {
                currentPage++;
            }
            fetchApiData();
        }

        if (nextItemIndex < items.size()) {
            return items.get(nextItemIndex++);
        }
        log.info(">>> API 응답 완료");
        return null;
    }

    private void fetchApiData() {

        log.info(">>>> [Batch Reader] 호출 URL 생성 중... lawdCd={}, dealYmd={}, key={}", lawdCd, dealYmd, serviceKey);

        String xmlResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https") // 명시적 설정
                        .host("apis.data.go.kr") // 명시적 설정
                        .path("/1613000/RTMSDataSvcAptTradeDev/getRTMSDataSvcAptTradeDev")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("LAWD_CD", lawdCd)
                        .queryParam("DEAL_YMD", dealYmd)
                        .queryParam("numOfRows", 100)
                        .queryParam("pageNo", currentPage)
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.debug(">>>> [Batch Reader] RAW XML 응답: {}", xmlResponse);
        try {

            ApiResponse<ApartmentTradeSaleItem> res = xmlMapper.readValue(xmlResponse, new TypeReference<ApiResponse<ApartmentTradeSaleItem>>() {
            });
            if (res != null && res.body() != null) {
                this.totalCount = res.body().totalCount();
                this.items = res.body().items().item() != null ? res.body().items().item() : new ArrayList<>();
                this.nextItemIndex = 0;
            } else {
                this.items = new ArrayList<>();
                this.totalCount = 0;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("XML 파싱 에러", e);
        }

    }
}
