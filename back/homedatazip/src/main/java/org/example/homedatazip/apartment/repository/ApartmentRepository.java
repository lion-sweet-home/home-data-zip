package org.example.homedatazip.apartment.repository;

import org.example.homedatazip.apartment.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    List<Apartment> findAllByAptSeqIn(Collection<String> aptSeqs);

    // [추가] 단건 조회를 위한 메서드 (중복 에러 방어용)
    Optional<Apartment> findByAptSeq(String aptSeq);

    // [추가] 존재 여부만 빠르게 확인하기 위한 메서드
    boolean existsByAptSeq(String aptSeq);
}
