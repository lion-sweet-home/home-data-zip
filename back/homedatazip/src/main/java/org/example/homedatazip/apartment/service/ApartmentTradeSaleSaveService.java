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
public class ApartmentTradeSaleSaveService { // 별도의 클래스로 분리!
    private final ApartmentRepository apartmentRepository;
    private final EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = {DataIntegrityViolationException.class})
    public Apartment saveAndGetApartment(ApartmentTradeSaleItem item, CoordinateInfoResponse response) {
        String aptSeq = item.getAptSeq();

        // 1. 세션에 남아있는 찌꺼기 제거 (캐시 충돌 방지)
        em.clear();

        try {
            // 2. DB에서 직접 조회 (영속성 컨텍스트를 거치지 않음)
            return apartmentRepository.findByAptSeq(aptSeq)
                    .orElseGet(() -> {
                        Apartment newApt = Apartment.create(item, response);
                        apartmentRepository.saveAndFlush(newApt);
                        return newApt;
                    });
        } catch (Exception e) {
            log.info(">>> [RETRY-FINAL] 중복 발생, 강제 세션 클리어 후 재조회: {}", aptSeq);
            // 3. 에러 발생 시 세션을 아예 밀어버리고 다시 조회
            em.clear();
            return apartmentRepository.findByAptSeq(aptSeq).orElse(null);
        }
    }
}
