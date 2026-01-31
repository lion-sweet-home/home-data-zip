package org.example.homedatazip.busstation.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.busstation.repository.BusStationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BusStationIngestService {

    private final BusStationRepository busStationRepository;

    // 지오코더 만들기 전 메소드
    // region = null
    public void upsertRowsWithoutRegion(List<SeoulBusStopResponse.Row> rows) {

        List<String> nodeIds = rows.stream()
                .map(SeoulBusStopResponse.Row::NODE_ID)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();

        Map<String, BusStation> existingMap = busStationRepository.findByNodeIdIn(nodeIds).stream()
                .collect(Collectors.toMap(BusStation::getNodeId, s -> s));

        List<BusStation> toSave = new ArrayList<>(rows.size());

        for (SeoulBusStopResponse.Row r : rows) {
            String nodeId = safeTrim(r.NODE_ID());
            if (nodeId == null) continue;

            String name = safeTrim(r.STOPS_NM());
            if (name == null) continue;

            Double longitude = parseDoubleOrNull(r.XCRD());
            Double latitude = parseDoubleOrNull(r.YCRD());

            BusStation station = existingMap.getOrDefault(nodeId, new BusStation(nodeId));

            // 당장 지오코더 없으니 region = null
            station.update(
                    safeTrim(r.STOPS_NO()),
                    name,
                    longitude,
                    latitude,
                    null
            );

            toSave.add(station);
        }

        busStationRepository.saveAll(toSave);
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Double parseDoubleOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
