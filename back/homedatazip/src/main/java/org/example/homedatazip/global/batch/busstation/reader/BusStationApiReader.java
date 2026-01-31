package org.example.homedatazip.global.batch.busstation.reader;

import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.client.SeoulBusOpenApiClient;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Component
@StepScope
@Slf4j
public class BusStationApiReader implements ItemReader<SeoulBusStopResponse.Row> {

    private final SeoulBusOpenApiClient client;

    //row 1000개씩
    private static final int PAGE_SIZE = 1000;

    private final Deque<SeoulBusStopResponse.Row> buffer = new ArrayDeque<>();

    private boolean initialized = false;
    private boolean isEnd = false;

    private int totalCount = 0;
    private int nextStart = 1;

    public BusStationApiReader(SeoulBusOpenApiClient client) {
        this.client = client;
    }

    @Override
    public SeoulBusStopResponse.Row read() {
        if (isEnd && buffer.isEmpty()) {
            return null;
        }

        if (!initialized) {
            initTotalCount();
            initialized = true;

            if (totalCount <= 0) {
                isEnd = true;
                return null;
            }
        }

        if (buffer.isEmpty()) {
            fetchNextRange();
        }

        return buffer.pollFirst();
    }

    private void initTotalCount() {
        SeoulBusStopResponse res = client.fetch(1, 1);

        if (res == null || res.busStopLocationXyInfo() == null) {
            totalCount = 0;
            log.info("버스정류장 totalCount 조회 실패 (응답 null)");
            return;
        }

        totalCount = res.busStopLocationXyInfo().list_total_count();
        log.info("버스정류장 totalCount={}", totalCount);
    }

    private void fetchNextRange() {
        if (nextStart > totalCount) {
            isEnd = true;
            return;
        }

        int start = nextStart;
        int end = Math.min(nextStart + PAGE_SIZE - 1, totalCount);
        nextStart = end + 1;

        SeoulBusStopResponse res = client.fetch(start, end);

        List<SeoulBusStopResponse.Row> rows =
                (res == null || res.busStopLocationXyInfo() == null) ? null : res.busStopLocationXyInfo().row();

        if (rows == null || rows.isEmpty()) {
            log.info("버스정류장 row 비어있음 (start={}, end={})", start, end);
            return;
        }

        log.info("버스정류장 {}건 로드 (start={}, end={})", rows.size(), start, end);
        buffer.addAll(rows);
    }
}
