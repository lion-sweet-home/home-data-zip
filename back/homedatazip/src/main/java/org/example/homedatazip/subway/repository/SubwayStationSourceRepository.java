package org.example.homedatazip.subway.repository;

import org.example.homedatazip.subway.entity.SubwayStationSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubwayStationSourceRepository extends JpaRepository<SubwayStationSource, Long> {

    List<SubwayStationSource> findAllByOrderByStationNameAscIdAsc();
}
