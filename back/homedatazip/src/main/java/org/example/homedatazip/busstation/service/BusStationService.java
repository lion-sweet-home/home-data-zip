package org.example.homedatazip.busstation.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.busstation.repository.BusStationRepository;
import org.example.homedatazip.data.Region;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BusStationService {

    private final BusStationRepository busStationRepository;

    public void upsertAll(List<BusStation> stations) {
        for (BusStation incoming : stations) {
            BusStation station = busStationRepository.findByNodeId(incoming.getNodeId())
                    .orElseGet(() -> new BusStation(incoming.getNodeId()));

            station.update(
                    incoming.getStationNumber(),
                    incoming.getName(),
                    incoming.getLongitude(),
                    incoming.getLatitude(),
                    incoming.getRegion()
            );

            busStationRepository.save(station);
        }
    }
}
