package org.example.homedatazip.subway.repository;

import org.example.homedatazip.subway.entity.SubwayStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubwayStationRepository extends JpaRepository<SubwayStation, Long> {

    Optional<SubwayStation> findByStationName(String stationName);
}
