package org.example.homedatazip.global.batch.busstation.reader;

import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse.Row;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Component
@StepScope
@Slf4j
public class BusStationApiReader implements ItemReader<Row> {

    private final WebClient webClient;

    @Value("${api.seoul.busstop.key}")
    private String apiKey;

    private int startIndex = 1;
    private final int pageSize = 1000;

    private final List<Row> buffer = new ArrayList<>();
    private boolean isEnd = false;

    public BusStationApiReader(WebClient.Builder builder) {
        // baseUrl은 니가 쓰는 서울 API 주소로 맞춰
        this.webClient = builder.baseUrl("http://openapi.seoul.go.kr:8088").build();
    }

    @Override
    public Row read() {
        if (isEnd && buffer.isEmpty()) return null;
        if (buffer.isEmpty()) fetch();
        return buffer.isEmpty() ? null : buffer.remove(0);
    }

    private void fetch() {
        int endIndex = startIndex + pageSize - 1;

        // URL 예시: /{KEY}/json/busStopLocationXyInfo/{start}/{end}
        SeoulBusStopResponse res = webClient.get()
                .uri(uri -> uri.path("/{key}/json/busStopLocationXyInfo/{start}/{end}")
                        .build(apiKey, startIndex, endIndex))
                .retrieve()
                .bodyToMono(SeoulBusStopResponse.class)
                .block();

        if (res == null || res.busStopLocationXyInfo() == null || res.busStopLocationXyInfo().row() == null) {
            isEnd = true;
            return;
        }

        var rows = res.busStopLocationXyInfo().row();
        if (rows.isEmpty()) {
            isEnd = true;
            return;
        }

        buffer.addAll(rows);

        log.info("[BUS] fetched {} rows ({}~{})", rows.size(), startIndex, endIndex);

        startIndex += pageSize;

        int total = res.busStopLocationXyInfo().listTotalCount();
        if (startIndex > total) isEnd = true;
    }
}
