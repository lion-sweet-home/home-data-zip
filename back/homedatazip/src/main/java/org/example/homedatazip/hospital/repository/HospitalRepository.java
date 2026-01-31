package org.example.homedatazip.hospital.repository;

import org.example.homedatazip.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    @Query("""
            select h.typeName, count(h) from Hospital h
            where h.gu = :gu and h.dong = :dong
            group by h.typeName
            order by count(h) desc 
            """)
    List<Object[]> countByTypeNameAndGuAndDong(
            @Param("gu") String gu,
            @Param("dong") String dong
    );

    Long countByGuAndDong(String gu, String dong);
}
