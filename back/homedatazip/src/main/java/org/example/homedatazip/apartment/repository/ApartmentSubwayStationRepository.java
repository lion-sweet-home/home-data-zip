package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.entity.ApartmentSubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApartmentSubwayStationRepository extends JpaRepository<ApartmentSubwayStation, Long> {

    /** 아파트 기준: 반경(km) 이내 지하철역 목록 (거리 오름차순) */
    List<ApartmentSubwayStation> findByApartmentIdAndDistanceKmLessThanEqualOrderByDistanceKmAsc(
            Long apartmentId, double maxDistanceKm);

    /** 역 기준: 반경(km) 이내 아파트 목록 (거리 오름차순) */
    List<ApartmentSubwayStation> findBySubwayStationIdAndDistanceKmLessThanEqualOrderByDistanceKmAsc(
            Long subwayStationId, double maxDistanceKm);

    /** (아파트, 역) 쌍으로 조회 - 배치 upsert 시 기존 행 여부 확인용 */
    Optional<ApartmentSubwayStation> findByApartmentIdAndSubwayStationId(Long apartmentId, Long subwayStationId);
}
