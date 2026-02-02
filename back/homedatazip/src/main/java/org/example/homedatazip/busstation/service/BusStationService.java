package org.example.homedatazip.busstation.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.dto.NearbyBusStationResponse;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.busstation.repository.BusStationRepository;
import org.example.homedatazip.global.geo.GeoBox;
import org.example.homedatazip.global.geo.Haversine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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

    @Transactional(readOnly = true)
    public List<NearbyBusStationResponse> findNearby(double baseLat, double baseLon, int radiusMeters, int limit) {
        GeoBox.BoundingBox box = GeoBox.boundingBox(baseLat, baseLon, radiusMeters);

        List<BusStation> candidates = busStationRepository.findCandidates(
                box.minLat(), box.maxLat(),
                box.minLon(), box.maxLon()
        );

        return candidates.stream()
                .map(bs -> {
                    double dist = Haversine.distanceMeters(baseLat, baseLon, bs.getLatitude(), bs.getLongitude());
                    return new NearbyBusStationResponse(
                            bs.getId(),
                            bs.getNodeId(),
                            bs.getStationNumber(),
                            bs.getName(),
                            bs.getLongitude(),
                            bs.getLatitude(),
                            dist
                    );
                })
                .filter(r -> r.distanceMeters() <= radiusMeters)
                .sorted(Comparator.comparingDouble(NearbyBusStationResponse::distanceMeters))
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NearbyBusStationResponse> findNearby500m(double baseLat, double baseLon, int limit) {
        return findNearby(baseLat, baseLon, 500, limit);
    }
}
