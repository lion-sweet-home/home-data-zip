package org.example.homedatazip.subway.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.entity.ApartmentSubwayDistance;
import org.example.homedatazip.apartment.repository.ApartmentSubwayDistanceRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubwayErrorCode;
import org.example.homedatazip.subway.dto.ApartmentNearSubwayResponse;
import org.example.homedatazip.subway.dto.NearbySubwayResponse;
import org.example.homedatazip.subway.dto.SubwayStationResponse;
import org.example.homedatazip.subway.entity.SubwayStation;
import org.example.homedatazip.subway.entity.SubwayStationSource;
import org.example.homedatazip.subway.repository.SubwayStationSourceRepository;
import org.example.homedatazip.subway.repository.SubwayStationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class SubwayStationService {

    private static final Set<Double> ALLOWED_RADIUS_KM = Set.of(0.5, 1.0, 2.0, 3.0, 5.0, 10.0);
    private static final int NEARBY_SUBWAY_TOP3 = 3;
    
    private final SubwayStationSourceRepository subwayStationSourceRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final ApartmentSubwayDistanceRepository apartmentSubwayDistanceRepository;

    /** 역명·호선 조건으로 지하철 역 검색 */
    @Transactional(readOnly = true)
    public List<SubwayStationResponse> searchStations(String stationName, String lineName) {
        // 역명과 호선명을 정규화하여 검색
        String trimmedStation = stationName != null ? stationName.trim() : "";
        String trimmedLine = lineName != null ? lineName.trim() : "";
        boolean hasStationName = StringUtils.hasText(trimmedStation);
        boolean hasLineName = StringUtils.hasText(trimmedLine);
        
        // 역명과 호선명이 모두 없으면 빈 리스트를 반환
        List<SubwayStationSource> sources;
        if (!hasStationName && !hasLineName) {
            return List.of();
        }

        // 역명, 호선 받은 것 중에 포함하는 역을 검색
        if (hasStationName && hasLineName) {
            sources = subwayStationSourceRepository.findByStationNameContainingAndLineNameWithStation(trimmedStation, trimmedLine);
        } else if (hasStationName) {
            sources = subwayStationSourceRepository.findByStationNameContainingWithStation(trimmedStation);
        } else {
            sources = subwayStationSourceRepository.findByLineNameWithStation(trimmedLine);
        }

        return groupByStationAndToResponse(sources);
    }

    /** 지하철 역 반경(km) 이내 아파트 목록 (거리 오름차순) */
    @Transactional(readOnly = true)
    public List<ApartmentNearSubwayResponse> findApartmentsNearStation(Long stationId, double distanceKm) {
        if (!ALLOWED_RADIUS_KM.contains(distanceKm)) {
            throw new BusinessException(SubwayErrorCode.INVALID_RADIUS);
        }
        subwayStationRepository.findById(stationId)
                .orElseThrow(() -> new BusinessException(SubwayErrorCode.STATION_NOT_FOUND));

        List<ApartmentSubwayDistance> distances = apartmentSubwayDistanceRepository
                .findBySubwayStationIdAndDistanceKmLessThanEqualOrderByDistanceKmAscWithApartment(stationId, distanceKm);

        return distances.stream()
                .map(this::toApartmentNearSubwayResponse)
                .toList();
    }

    /** 아파트 기준 가까운 지하철역 top 3 */
    @Transactional(readOnly = true)
    public List<NearbySubwayResponse> findNearbySubwaysByApartmentId(Long apartmentId) {
        List<ApartmentSubwayDistance> distances = apartmentSubwayDistanceRepository
                .findByApartmentIdWithSubwayStation(apartmentId, 10.0);

        List<ApartmentSubwayDistance> top3 = distances.stream().limit(NEARBY_SUBWAY_TOP3).toList();
        if (top3.isEmpty()) {
            return List.of();
        }

        List<Long> stationIds = top3.stream()
                .map(asd -> asd.getSubwayStation().getId())
                .distinct()
                .toList();

        List<SubwayStationSource> allSources = subwayStationSourceRepository
                .findByStationIdInWithStation(stationIds);

        Map<Long, List<String>> lineNamesByStationId = allSources.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStation().getId(),
                        mapping(SubwayStationSource::getLineName, toList())
                ));

        return top3.stream()
                .map(asd -> toNearbySubwayResponse(asd, lineNamesByStationId))
                .toList();
    }

    // 가까운 지하철역 목록 반환
    private NearbySubwayResponse toNearbySubwayResponse(ApartmentSubwayDistance asd, Map<Long, List<String>> lineNamesByStationId) {
        SubwayStation station = asd.getSubwayStation();
        List<String> lineNames = lineNamesByStationId.getOrDefault(station.getId(), List.of())
                .stream()
                .distinct()
                .sorted()
                .toList();
        double distanceKm = Math.round(asd.getDistanceKm() * 1000.0) / 1000.0;
        return new NearbySubwayResponse(
                station.getStationName(),
                lineNames,
                distanceKm
        );
    }

    // 역명 단위로 그룹핑하여, 해당 역의 호선 목록과 대표 좌표를 반환
    private List<SubwayStationResponse> groupByStationAndToResponse(List<SubwayStationSource> sources) {
        return sources.stream()
                .collect(groupingBy(s -> s.getStation().getId()))
                .values().stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(SubwayStationResponse::stationName))
                .toList();
    }

    // 해당 역의 호선 목록과 대표 좌표를 반환
    private SubwayStationResponse toResponse(List<SubwayStationSource> group) {
        SubwayStationSource first = group.getFirst();
        SubwayStation station = first.getStation();
        List<String> lineNames = group.stream()
                .map(SubwayStationSource::getLineName)
                .distinct()
                .sorted()
                .toList();
        return new SubwayStationResponse(
                station.getId(),
                station.getStationName(),
                lineNames,
                station.getLatitude(),
                station.getLongitude()
        );
    }
    
    // 아파트 목록 반환
    private ApartmentNearSubwayResponse toApartmentNearSubwayResponse(ApartmentSubwayDistance asd) {
        Apartment apt = asd.getApartment();
        return new ApartmentNearSubwayResponse(
                apt.getId(),
                apt.getAptName(),
                apt.getRoadAddress(),
                apt.getJibunAddress(),
                apt.getLatitude(),
                apt.getLongitude(),
                apt.getBuildYear(),
                asd.getDistanceKm()
        );
    }
}
