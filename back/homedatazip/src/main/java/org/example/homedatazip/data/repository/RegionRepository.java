package org.example.homedatazip.data.repository;

import org.example.homedatazip.data.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    // 청크 단위 조회
    List<Region> findByLawdCodeIn(List<String> lawdCodes);

    // 아파트 API 호출을 위한 5자리 코드 리스트 (중복 제거)
    @Query("SELECT DISTINCT r.sggCode FROM Region r")
    List<String> findDistinctSggCode();

    // 중복 없는 시/도 리스트
    @Query("SELECT DISTINCT r.sido FROM Region r")
    List<String> findDistinctSido();

    // 특정 시/도 내의 중복 없는 구/군 리스트
    @Query("SELECT DISTINCT r.gugun FROM Region r WHERE r.sido = :sido")
    List<String> findDistinctGugunBySido(String sido);

    // 시와 구를 조건으로 검색한 동 리스트
    List<Region> findBySidoAndGugun(String sido, String gugun);

    // 특정 법정동 코드 존재 여부 확인
    Optional<Region> findByLawdCode(String lawdCode);
}
