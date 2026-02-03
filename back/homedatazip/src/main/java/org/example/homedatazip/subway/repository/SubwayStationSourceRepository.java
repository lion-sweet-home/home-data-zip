package org.example.homedatazip.subway.repository;

import org.example.homedatazip.subway.entity.SubwayStationSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubwayStationSourceRepository extends JpaRepository<SubwayStationSource, Long> {

    List<SubwayStationSource> findAllByOrderByStationNameAscIdAsc();

    /** 역명 포함 검색 (station fetch, 역명은 한글이라 대소문자 없음) */
    @Query("SELECT s FROM SubwayStationSource s JOIN FETCH s.station st WHERE st.id IS NOT NULL AND s.stationName LIKE CONCAT('%', :stationName, '%') ORDER BY s.stationName, s.id")
    List<SubwayStationSource> findByStationNameContainingWithStation(@Param("stationName") String stationName);

    /** 역명 + 호선 검색 (station fetch) */
    @Query("SELECT s FROM SubwayStationSource s JOIN FETCH s.station st WHERE st.id IS NOT NULL AND s.stationName LIKE CONCAT('%', :stationName, '%') AND s.lineName = :lineName ORDER BY s.stationName, s.id")
    List<SubwayStationSource> findByStationNameContainingAndLineNameWithStation(@Param("stationName") String stationName, @Param("lineName") String lineName);

    /** 호선만 검색 (station fetch) */
    @Query("SELECT s FROM SubwayStationSource s JOIN FETCH s.station st WHERE st.id IS NOT NULL AND s.lineName = :lineName ORDER BY s.stationName, s.id")
    List<SubwayStationSource> findByLineNameWithStation(@Param("lineName") String lineName);
}
