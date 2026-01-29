package org.example.homedatazip.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region,Long> {
    Optional<Region> findByLawdCode(String lawdCode);
}
