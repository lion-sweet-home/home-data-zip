package org.example.homedatazip.busstation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.busstation.repository.BusStationRepository;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusStationIngestService {

    private final BusStationRepository busStationRepository;
    private final GeoService geoService;

    @Transactional
    public void upsert(SeoulBusStopResponse.Row row) {
        // 좌표 파싱
        Double lon = parseDouble(row.XCRD()); // 경도
        Double lat = parseDouble(row.YCRD()); // 위도

        if (lat == null || lon == null) {
            log.warn("좌표 누락 - nodeId={}, name={}", row.NODE_ID(), row.STOPS_NM());
            return;
        }

        Region region;
        try {
            region = geoService.convertAddressInfo(lat, lon); // ✅ 여기서 lawdCode 매핑 끝
        } catch (Exception e) {
            // 카카오 호출 실패/Region 못찾음 등
            log.warn("Region 매핑 실패 - nodeId={}, lat={}, lon={}, err={}",
                    row.NODE_ID(), lat, lon, e.getMessage());
            region = null; // region 없이라도 저장하고 싶으면 null로 두고 진행
        }

        BusStation station = busStationRepository.findByNodeId(row.NODE_ID())
                .orElseGet(() -> new BusStation(row.NODE_ID()));

        station.update(
                row.STOPS_NO(),      // ARS-ID
                row.STOPS_NM(),      // 이름
                lon,                 // longitude
                lat,                 // latitude
                region
        );

        busStationRepository.save(station);
    }

    private Double parseDouble(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
