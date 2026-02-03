
package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    /** 위/경도가 있는 아파트만 조회 (아파트–지하철 거리 배치용) */
    List<Apartment> findByLatitudeIsNotNullAndLongitudeIsNotNull();

    List<Apartment> findAllByAptSeqIn(Collection<String> aptSeqs);

    // [추가] 단건 조회를 위한 메서드 (중복 에러 방어용)
    Optional<Apartment> findByAptSeq(String aptSeq);

    // [추가] 존재 여부만 빠르게 확인하기 위한 메서드
    boolean existsByAptSeq(String aptSeq);

    @Modifying
    @Transactional
    @Query(value = """
    INSERT IGNORE INTO apartments 
    (apt_name, road_address, jibun_address, latitude, longitude, build_year, apt_seq, region_id) 
    VALUES (:name, :road, :jibun, :lat, :lon, :year, :seq, :regionId)
    """, nativeQuery = true)
    void insertIgnore(@Param("name") String name,
                      @Param("road") String road,
                      @Param("jibun") String jibun,
                      @Param("lat") Double lat,
                      @Param("lon") Double lon,
                      @Param("year") Integer year,
                      @Param("seq") String seq,
                      @Param("regionId") Long regionId);
}