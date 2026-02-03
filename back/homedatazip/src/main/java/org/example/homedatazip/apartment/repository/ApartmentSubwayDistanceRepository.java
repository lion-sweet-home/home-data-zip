package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.entity.ApartmentSubwayDistance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApartmentSubwayDistanceRepository extends JpaRepository<ApartmentSubwayDistance, Long> {

    /** 아파트 기준: 반경(km) 이내 지하철역 목록 (거리 오름차순) */
    List<ApartmentSubwayDistance> findByApartmentIdAndDistanceKmLessThanEqualOrderByDistanceKmAsc(
            Long apartmentId, double maxDistanceKm);

    /** 역 기준: 반경(km) 이내 아파트 목록 (거리 오름차순) */
    List<ApartmentSubwayDistance> findBySubwayStationIdAndDistanceKmLessThanEqualOrderByDistanceKmAsc(
            Long subwayStationId, double maxDistanceKm);

    /** (아파트, 역) 쌍으로 조회 - 배치 upsert 시 기존 행 여부 확인용 */
    Optional<ApartmentSubwayDistance> findByApartmentIdAndSubwayStationId(Long apartmentId, Long subwayStationId);
}
