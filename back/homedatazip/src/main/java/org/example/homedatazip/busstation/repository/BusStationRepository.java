package org.example.homedatazip.busstation.repository;

import org.example.homedatazip.busstation.entity.BusStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BusStationRepository extends JpaRepository<BusStation, Long> {

    Optional<BusStation> findByNodeId(String nodeId);

    List<BusStation> findTop500ByRegionIsNullOrderByIdAsc();

    @Query("""
        select b
        from BusStation b
        where b.longitude between :minLat and :maxLat
          and b.latitude between :minLon and :maxLon
    """)
    List<BusStation> findCandidates(
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLon") double minLon,
            @Param("maxLon") double maxLon
    );
}
