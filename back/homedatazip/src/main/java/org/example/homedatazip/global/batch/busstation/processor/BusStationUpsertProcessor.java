package org.example.homedatazip.global.batch.busstation.processor;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse.Row;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.busstation.repository.BusStationRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusStationUpsertProcessor implements ItemProcessor<Row, BusStation> {

    private final BusStationRepository busStationRepository;

    @Override
    public BusStation process(Row row) {
        if (row == null) return null;

        String nodeId = safeTrim(row.NODE_ID());
        if (nodeId == null) return null;

        Double longitude = parseDouble(row.XCRD());
        Double latitude  = parseDouble(row.YCRD());

        if (longitude == null || latitude == null) return null;

        BusStation station = busStationRepository.findByNodeId(nodeId)
                .orElseGet(() -> new BusStation(nodeId));

        // 기존 region 유지 (업서트 배치가 region을 null로 덮어쓰면 안 됨)
        var currentRegion = station.getRegion();

        station.update(
                safeTrim(row.STOPS_NO()),
                safeTrim(row.STOPS_NM()),
                longitude,
                latitude,
                currentRegion
        );

        return station;
    }

    private String safeTrim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private Double parseDouble(String v) {
        String t = safeTrim(v);
        if (t == null) return null;
        try {
            return Double.parseDouble(t);
        } catch (Exception e) {
            return null;
        }
    }
}
