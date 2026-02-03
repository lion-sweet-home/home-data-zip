package org.example.homedatazip.subway.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.subway.dto.ApartmentNearSubwayResponse;
import org.example.homedatazip.subway.dto.SubwayStationSearchRequest;
import org.example.homedatazip.subway.dto.SubwayStationSearchResponse;
import org.example.homedatazip.subway.dto.SubwayStationResponse;
import org.example.homedatazip.subway.service.SubwayStationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subway/stations")
public class SubwayStationController {

    private final SubwayStationService subwayStationService;

    // 지하철 역 검색 (한 검색란에 역명 또는 호선으로 검색, 둘 다 optional).
    // GET /api/subway/stations?stationName={stationName}&lineName={lineName}
    @GetMapping()
    public ResponseEntity<SubwayStationSearchResponse> searchStations(
            @ModelAttribute SubwayStationSearchRequest request
    ) {
        List<SubwayStationResponse> stations = subwayStationService.searchStations(request.stationName(), request.lineName());
        return ResponseEntity.ok(new SubwayStationSearchResponse(stations));
    }

    // 해당 지하철 역 반경 내 아파트 검색.
    // GET /api/subway/stations/{stationId}/apartments?distanceKm={distanceKm}
    @GetMapping("/{stationId}/apartments")
    public ResponseEntity<List<ApartmentNearSubwayResponse>> getApartmentsNearStation(
            @PathVariable Long stationId, @RequestParam double distanceKm
    ) {
        List<ApartmentNearSubwayResponse> apartments = subwayStationService.findApartmentsNearStation(stationId, distanceKm);
        return ResponseEntity.ok(apartments);
    }
}
