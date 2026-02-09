package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.entity.ApartmentSchoolDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApartmentSchoolDistanceRepository extends JpaRepository<ApartmentSchoolDistance, Long> {
    /** 아파트 기준: 반경(km) 이내 학교 목록 (거리 오름차순) */
    List<ApartmentSchoolDistance> findByApartmentIdAndDistanceKmLessThanEqualOrderByDistanceKmAsc(
            Long apartmentId, double maxDistanceKm);

    /** 학교 기준: 반경(km) 이내 아파트 목록 (거리 오름차순) */
    List<ApartmentSchoolDistance> findBySchoolIdAndDistanceKmLessThanEqualOrderByDistanceKmAsc(
            Long schoolId, double maxDistanceKm);

    /** 학교 기준: 반경(km) 이내 아파트 목록 (apartment fetch, 거리 오름차순) */
    @Query("SELECT asd FROM ApartmentSchoolDistance asd JOIN FETCH asd.apartment WHERE asd.school.id = :schoolId AND asd.distanceKm <= :maxDistanceKm ORDER BY asd.distanceKm")
    List<ApartmentSchoolDistance> findBySchoolIdAndDistanceKmLessThanEqualOrderByDistanceKmAscWithApartment(
            @Param("schoolId") Long schoolId, @Param("maxDistanceKm") double maxDistanceKm);

}
