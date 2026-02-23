package org.example.homedatazip.hospital.repository;

import org.example.homedatazip.data.Region;
import org.example.homedatazip.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Optional<Hospital> findByHospitalId(String hospitalId);

    @Query("""
            select count(h) from Hospital h
            where h.region.sido = :sido
            and h.region.gugun = :gugun
            and h.region.dong = :dong
            """)
    Long countByRegion(
            @Param("sido") String sido,
            @Param("gugun") String gugun,
            @Param("dong") String dong
    );

    List<Hospital> findByRegion(Region region);

    List<Hospital> findByRegionIsNullAndLatitudeIsNotNullAndLongitudeIsNotNull();
}
