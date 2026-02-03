package org.example.homedatazip.apartment.service;


import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.global.geocode.dto.CoordinateInfoResponse;
import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class ApartmentTradeSaleSaveService {
    private final ApartmentRepository apartmentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Apartment saveAndGetApartment(ApartmentTradeSaleItem item, CoordinateInfoResponse response) {
        // 1. 중복 에러 발생 가능성을 원천 차단 (Native SQL)
        apartmentRepository.insertIgnore(
                item.getAptNm(),
                response.roadAddress(),
                response.jibunAddress(),
                response.latitude(),
                response.longitude(),
                Integer.parseInt(item.getBuildYear()),
                item.getAptSeq(),
                response.region() != null ? response.region().getId() : null
        );

        // 2. 깨끗한 세션에서 데이터 조회
        return apartmentRepository.findByAptSeq(item.getAptSeq())
                .orElseThrow(() -> new RuntimeException("아파트 처리 실패: " + item.getAptSeq()));
    }
}