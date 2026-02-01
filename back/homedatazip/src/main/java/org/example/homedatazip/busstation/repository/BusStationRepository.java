package org.example.homedatazip.busstation.repository;

import org.example.homedatazip.busstation.entity.BusStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusStationRepository extends JpaRepository<BusStation, Long> {

    Optional<BusStation> findByNodeId(String nodeId);

    boolean existsByNodeId(String nodeId);
}
