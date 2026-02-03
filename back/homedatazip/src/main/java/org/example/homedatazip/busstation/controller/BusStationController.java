package org.example.homedatazip.busstation.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.dto.NearbyBusStationReponse;
import org.example.homedatazip.busstation.service.BusStationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bus-stations")
public class BusStationController {

    private final BusStationService busStationService;

    // 아파트 기준 근처 정류장 조회
    @GetMapping("/nearby/apartments/{apartmentId}")
    public ResponseEntity<Map<String, Object>> nearbyByApartment(
            @PathVariable Long apartmentId,
            @RequestParam(defaultValue = "500") int radiusMeters,
            @RequestParam(defaultValue = "50") int limit
    ) {
        var list = busStationService.findNearbyByApartmentId(apartmentId, radiusMeters, limit);

        return ResponseEntity.ok(Map.of(
                "count", list.size(),
                "items", list
        ));
    }

    // 좌표 기준 근처 정류장 조회 (확장용)
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyBusStationReponse>> nearbyByCoordinate(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "500") int radiusMeters,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(
                busStationService.findNearby(lat, lon, radiusMeters, limit)
        );
    }
}
