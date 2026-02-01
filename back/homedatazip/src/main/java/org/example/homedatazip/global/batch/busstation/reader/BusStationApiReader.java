package org.example.homedatazip.global.batch.busstation.reader;

import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.client.SeoulBusOpenApiClient;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@StepScope
public class BusStationApiReader implements ItemReader<SeoulBusStopResponse.Row> {

    private final SeoulBusOpenApiClient client;

    private final int pageSize = 1000;

    private int startIndex = 1;
    private boolean isEnd = false;

    private List<SeoulBusStopResponse.Row> buffer = new ArrayList<>();

    public BusStationApiReader(SeoulBusOpenApiClient client) {
        this.client = client;
    }

    @Override
    public SeoulBusStopResponse.Row read() {
        if (isEnd && buffer.isEmpty()) return null;

        if (buffer.isEmpty()) {
            fetch();
        }

        return buffer.isEmpty() ? null : buffer.remove(0);
    }

    private void fetch() {
        int endIndex = startIndex + pageSize - 1;

        SeoulBusStopResponse res = client.fetch(startIndex, endIndex);

        if (res == null || res.busStopLocationXyInfo() == null || res.busStopLocationXyInfo().row() == null) {
            isEnd = true;
            return;
        }

        List<SeoulBusStopResponse.Row> rows = res.busStopLocationXyInfo().row();
        if (rows.isEmpty()) {
            isEnd = true;
            return;
        }

        buffer.addAll(rows);

        int total = res.busStopLocationXyInfo().listTotalCount();
        log.info("[BUS_STATION] fetch start={} end={} size={} total={}", startIndex, endIndex, rows.size(), total);

        startIndex += pageSize;

        if (startIndex > total) {
            isEnd = true;
        }
    }
}
