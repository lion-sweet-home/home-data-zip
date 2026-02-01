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
        String nodeId = row.NODE_ID();
        if (nodeId == null || nodeId.isBlank()) return null;

        BusStation station = busStationRepository.findByNodeId(nodeId)
                .orElseGet(() -> new BusStation(nodeId));

        Double longitude = parseDouble(row.XCRD()); // 경도
        Double latitude = parseDouble(row.YCRD());  // 위도

        station.update(
                row.STOPS_NO(),
                row.STOPS_NM(),
                longitude,
                latitude,
                null // region은 여기서는 안 붙임
        );

        return station;
    }

    private Double parseDouble(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
    }
}
