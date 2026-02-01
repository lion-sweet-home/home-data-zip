package org.example.homedatazip.global.batch.busstation.reader;

import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse.Row;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Component
@StepScope
@Slf4j
public class BusStationApiReader implements ItemReader<Row> {

    private final WebClient webClient;

    @Value("${seoul.openapi.key}")
    private String apiKey;

    @Value("${seoul.openapi.type:json}")
    private String type; // 기본 json

    @Value("${seoul.openapi.service:busStopLocationXyInfo}")
    private String service; // 기본 서비스명

    private int startIndex = 1;
    private final int pageSize = 1000;

    private final List<Row> buffer = new ArrayList<>();
    private boolean isEnd = false;

    public BusStationApiReader(
            WebClient.Builder builder,
            @Value("${seoul.openapi.base-url}") String baseUrl
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Row read() {
        if (isEnd && buffer.isEmpty()) return null;
        if (buffer.isEmpty()) fetch();
        return buffer.isEmpty() ? null : buffer.remove(0);
    }

    private void fetch() {
        int endIndex = startIndex + pageSize - 1;

        String path = "/" + apiKey + "/" + type + "/" + service + "/" + startIndex + "/" + endIndex;

        SeoulBusStopResponse res = webClient.get()
                .uri(path)
                .retrieve()
                .onStatus(s -> s.isError(), cr ->
                        cr.bodyToMono(String.class)
                                .doOnNext(body -> log.error("[BUS] API error body={}", body))
                                .flatMap(body -> Mono.error(new RuntimeException("Seoul API error: " + body)))
                )
                .bodyToMono(SeoulBusStopResponse.class)
                .block();

        if (res == null || res.busStopLocationXyInfo() == null) {
            log.warn("[BUS] response null. end.");
            isEnd = true;
            return;
        }

        if (res.busStopLocationXyInfo().result() != null) {
            String code = res.busStopLocationXyInfo().result().code();
            String msg = res.busStopLocationXyInfo().result().message();
            if (code != null && !"INFO-000".equalsIgnoreCase(code)) {
                log.error("[BUS] RESULT not ok. code={}, msg={}", code, msg);
                isEnd = true;
                return;
            }
        }

        var rows = res.busStopLocationXyInfo().row();
        if (rows == null || rows.isEmpty()) {
            log.info("[BUS] empty rows. end.");
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
