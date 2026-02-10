package org.example.homedatazip.apartment.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.AptSummaryResponse;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.apartment.service.ApartmentService;
import org.example.homedatazip.busstation.dto.NearbyBusStationReponse;
import org.example.homedatazip.busstation.service.BusStationService;
import org.example.homedatazip.school.dto.NearbySchoolResponse;
import org.example.homedatazip.school.service.SchoolService;
import org.example.homedatazip.data.dto.ApartmentOptionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentRepository apartmentRepository;
    private final BusStationService busStationService;
    private final ApartmentService apartmentService;
    private final SchoolService schoolService;

    @GetMapping
    public ResponseEntity<List<ApartmentOptionResponse>> apartments(@RequestParam Long regionId) {
        List<Apartment> apartments = apartmentRepository.findByRegionIdOrderByAptNameAsc(regionId);

        List<ApartmentOptionResponse> result = apartments.stream()
                .map(a -> new ApartmentOptionResponse(a.getId(), a.getAptName()))
                .toList();

        return ResponseEntity.ok(result);
    }

    //반경 500m내 busStation 리스트 조회
    @GetMapping("/{apartmentId}/bus-stations")
    public ResponseEntity<Map<String, Object>> nearbyBusStations(
            @PathVariable Long apartmentId,
            @RequestParam(defaultValue = "500") int radiusMeters,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<NearbyBusStationReponse> list =
                busStationService.findNearbyByApartmentId(apartmentId, radiusMeters, limit);

        return ResponseEntity.ok(Map.of(
                "count", list.size(),
                "items", list
        ));
    }

    /** 아파트 기준 가까운 학교 top 3 (schoolLevel 옵션) */
    @GetMapping("/{apartmentId}/schools")
    public ResponseEntity<List<NearbySchoolResponse>> nearbySchools(
            @PathVariable Long apartmentId,
            @RequestParam(required = false) List<String> schoolLevel
    ) {
        List<String> levels = (schoolLevel != null) ? schoolLevel : List.of();
        List<NearbySchoolResponse> list = schoolService.findNearbySchoolsByApartmentId(apartmentId, levels);
        return ResponseEntity.ok(list);
    }

    /**
     * 아파트 키워드로 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<AptSummaryResponse>> searchByKeyword(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok()
                .body(apartmentService.searchByKeyword(keyword));
    }
}
