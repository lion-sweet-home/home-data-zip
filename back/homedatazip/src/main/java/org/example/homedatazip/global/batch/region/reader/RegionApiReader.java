package org.example.homedatazip.global.batch.region.reader;


import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.data.dto.ApiResponse;
import org.example.homedatazip.data.dto.RegionApiResponse;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;


@Component
@StepScope
@Slf4j
public class RegionApiReader implements ItemReader<RegionApiResponse> {

    private final WebClient webClient;

    @Value("${api.data-go-kr.service-key}")
    private String serviceKey;

    private int page = 1;
    private List<RegionApiResponse> buffer = new ArrayList<>();
    private boolean isEnd = false;

    public RegionApiReader(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.odcloud.kr/api").build();
    }

    @Override
    public RegionApiResponse read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

        if (isEnd && buffer.isEmpty()) {
            return null;
        }
        if (buffer.isEmpty()){
            fetch();
        }

        return buffer.isEmpty() ? null : buffer.remove(0);
    }

    private void fetch() {
        ApiResponse res = webClient.get()
                .uri(uri -> uri.path("/15063424/v1/uddi:5176efd5-da6e-42a0-b2cf-8512f74503ea")
                        .queryParam("page", page++)
                        .queryParam("perPage", 100)
                        .queryParam("serviceKey", serviceKey)
                        .build(false))
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                clientResponse.bodyToMono(String.class)
                        .doOnNext(errorBody -> log.error("API 에러 상세 메시지: {}", errorBody))
                        .flatMap(errorBody -> Mono.error(new RuntimeException("API 호출 실패: " + errorBody)))
        )
                .bodyToMono(ApiResponse.class).block();

        if (res == null || res.data() == null || res.data().isEmpty()) {
            isEnd = true;
        } else {
            log.info("성공적으로 {}개의 데이터를 가져왔습니다. (현재 페이지: {})", res.data().size(), page - 1);
            buffer.addAll(res.data());
        }

    }

}
