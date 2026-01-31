package org.example.homedatazip.busstation.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.busstation.repository.BusStationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BusStationService {

    private final BusStationRepository busStationRepository;

    public void upsertStationsWithoutRegion(List<BusStationApiResponse> items) {

        List<String> nodeIds = items.stream()
                .map(BusStationApiResponse::nodeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, BusStation> existingMap = busStationRepository.findByNodeIdIn(nodeIds).stream()
                .collect(Collectors.toMap(BusStation::getNodeId, s -> s));

        List<BusStation> toSave = new ArrayList<>(items.size());

        for (BusStationApiResponse dto : items) {
            if (dto.nodeId() == null || dto.nodeId().isBlank()) {
                continue;
            }
            if (dto.name() == null || dto.name().isBlank()) {
                continue;
            }

            BusStation station = existingMap.getOrDefault(dto.nodeId(), new BusStation(dto.nodeId()));

            station.update(
                    dto.stationNumber(),
                    dto.name(),
                    dto.longitude(),
                    dto.latitude(),
                    null
            );

            toSave.add(station);
        }

        busStationRepository.saveAll(toSave);
    }
}
