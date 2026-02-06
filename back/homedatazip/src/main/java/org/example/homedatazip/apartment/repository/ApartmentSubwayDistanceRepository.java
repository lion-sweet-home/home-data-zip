package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.entity.ApartmentSubwayDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApartmentSubwayDistanceRepository extends JpaRepository<ApartmentSubwayDistance, Long> {

    /** 아파트 기준: 반경(km) 이내 지하철역 목록 (거리 오름차순) */
    List<ApartmentSubwayDistance> findByApartmentIdAndDistanceKmLessThanEqualOrderByDistanceKmAsc(
            Long apartmentId, double maxDistanceKm);

    /** 역 기준: 반경(km) 이내 아파트 목록 (거리 오름차순) */
    List<ApartmentSubwayDistance> findBySubwayStationIdAndDistanceKmLessThanEqualOrderByDistanceKmAsc(
            Long subwayStationId, double maxDistanceKm);

    /** 역 기준: 반경(km) 이내 아파트 목록 (apartment fetch, 거리 오름차순) */
    @Query("SELECT asd FROM ApartmentSubwayDistance asd JOIN FETCH asd.apartment WHERE asd.subwayStation.id = :stationId AND asd.distanceKm <= :maxDistanceKm ORDER BY asd.distanceKm")
    List<ApartmentSubwayDistance> findBySubwayStationIdAndDistanceKmLessThanEqualOrderByDistanceKmAscWithApartment(
            @Param("stationId") Long stationId, @Param("maxDistanceKm") double maxDistanceKm);

}
