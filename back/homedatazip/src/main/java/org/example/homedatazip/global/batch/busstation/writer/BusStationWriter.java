package org.example.homedatazip.global.batch.busstation.writer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.busstation.service.BusStationService;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusStationWriter implements ItemWriter<BusStation> {

    private final BusStationService busStationService;

    @Override
    @Transactional
    public void write(Chunk<? extends BusStation> chunk) {
        List<BusStation> items = new ArrayList<>(chunk.getItems());
        if (items.isEmpty()) return;

        busStationService.upsertAll(items);
        log.info("[BUS_STATION] write size={}", items.size());
    }

    // 배치 4.x 쓰는 경우 대비 (필요 없으면 지워도 됨)
    @Transactional
    public void write(List<? extends BusStation> items) {
        if (items == null || items.isEmpty()) return;

        busStationService.upsertAll(new ArrayList<>(items));
        log.info("[BUS_STATION] write size={}", items.size());
    }
}
