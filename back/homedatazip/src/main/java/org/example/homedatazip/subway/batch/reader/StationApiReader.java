package org.example.homedatazip.subway.batch.reader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homedatazip.subway.batch.dto.SubwayStationSourceSync;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * OpenAPI 지하철 역 목록을 한 번 호출 후, read() 호출마다 한 건씩 반환.
 */
@Component
@StepScope
public class StationApiReader implements ItemStreamReader<SubwayStationSourceSync> {

    private final WebClient webClient;
    private final String apiUrl;
    private final ObjectMapper objectMapper;

    private List<SubwayStationSourceSync> items;
    private int index;

    public StationApiReader(
            WebClient webClient,
            @Value("${subway.openapi.url}") String apiUrl,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.apiUrl = apiUrl;
        this.objectMapper = objectMapper;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new ItemStreamException("subway.openapi.url 이 필수입니다.");
        }
        String body = webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (body == null || body.isBlank()) {
            items = List.of();
        } else {
            try {
                items = objectMapper.readValue(body, new TypeReference<List<SubwayStationSourceSync>>() {});
            } catch (Exception e) {
                throw new ItemStreamException("OpenAPI 응답 JSON 파싱 실패: " + apiUrl, e);
            }
        }
        index = 0;
    }

    @Override
    public SubwayStationSourceSync read() {
        if (items == null || index >= items.size()) {
            return null;
        }
        return items.get(index++);
    }

    @Override
    public void close() throws ItemStreamException {
        items = null;
        index = 0;
    }
}
