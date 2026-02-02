package org.example.homedatazip.busstation.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.busstation.dto.NearbyBusStationReponse;
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
    private final ApartmentRepository apartmentRepository;

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
    public List<NearbyBusStationReponse> findNearbyByApartmentId(Long apartmentId, int radiusMeters, int limit) {
        Apartment apt = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("apartment not found: " + apartmentId));

        // 좌표 유효 검증
        if (apt.getLatitude() == null || apt.getLongitude() == null) {
            throw new IllegalStateException("apartment coordinates missing: " + apartmentId);
        }

        return findNearby(apt.getLatitude(), apt.getLongitude(), radiusMeters, limit);
    }

    // 아파트 ID로 조회
    @Transactional(readOnly = true)
    public List<NearbyBusStationReponse> findNearbyByApartmentId500m(Long apartmentId, int limit) {
        return findNearbyByApartmentId(apartmentId, 500, limit);
    }

    // baseLat, baseLon 를 직접 입력받아 조회
    @Transactional(readOnly = true)
    public List<NearbyBusStationReponse> findNearby(double baseLat, double baseLon, int radiusMeters, int limit) {
        // 바운딩 박스
        GeoBox.BoundingBox box = GeoBox.boundingBox(baseLat, baseLon, radiusMeters);

        // 바운딩 박스 범위 안에 있는 정류장 조회
        List<BusStation> candidates = busStationRepository.findCandidates(
                box.minLat(), box.maxLat(),
                box.minLon(), box.maxLon()
        );

        return candidates.stream()
                .filter(bs -> bs.getLatitude() != null && bs.getLongitude() != null)
                .map(bs -> toResponse(baseLat, baseLon, bs))
                .filter(r -> r.distanceMeters() <= radiusMeters)
                .sorted(Comparator.comparingDouble(NearbyBusStationReponse::distanceMeters))
                .limit(limit)
                .toList();
    }

    private NearbyBusStationReponse toResponse(double baseLat, double baseLon, BusStation bs) {
        // 아파트좌표와 정류장 좌표를 이용해 하버사인으로 거리 계산
        double dist = Haversine.distanceMeters(baseLat, baseLon, bs.getLatitude(), bs.getLongitude());
        return new NearbyBusStationReponse(
                bs.getId(),
                bs.getNodeId(),
                bs.getStationNumber(),
                bs.getName(),
                bs.getLatitude(),
                bs.getLongitude(),
                dist
        );
    }
}
