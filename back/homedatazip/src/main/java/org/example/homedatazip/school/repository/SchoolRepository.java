package org.example.homedatazip.school.repository;

import org.example.homedatazip.school.entity.School;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByName(String name);

    /** 시도·구군(필수), 동(옵션)으로 학교 목록 조회. 동이 없으면 시도+구군 전체 */
    @Query("""
            SELECT s FROM School s JOIN s.region r
            WHERE r.sido = :sido AND r.gugun = :gugun
            AND (:dong IS NULL OR :dong = '' OR r.dong LIKE CONCAT(:dong, '%'))
            ORDER BY s.schoolLevel, s.name
            """)
    List<School> findByRegionSidoAndGugunAndDongOptional(
            @Param("sido") String sido,
            @Param("gugun") String gugun,
            @Param("dong") String dong
    );

    /** 시도·구군(필수), 동(옵션), schoolLevel으로 학교 목록 조회 */
    @Query("""
            SELECT s FROM School s JOIN s.region r
            WHERE r.sido = :sido AND r.gugun = :gugun
            AND (:dong IS NULL OR :dong = '' OR r.dong LIKE CONCAT(:dong, '%'))
            AND s.schoolLevel IN :schoolLevels
            ORDER BY s.schoolLevel, s.name
            """)
    List<School> findByRegionSidoAndGugunAndDongOptionalWithSchoolLevel(
            @Param("sido") String sido,
            @Param("gugun") String gugun,
            @Param("dong") String dong,
            @Param("schoolLevels") List<String> schoolLevels
    );

    /** 학교명 키워드로 검색 (부분일치, 이름 오름차순) */
    @Query("""
            SELECT s FROM School s
            WHERE s.name LIKE CONCAT('%', :keyword, '%')
            ORDER BY s.name
            """)
    List<School> searchByNameContaining(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
