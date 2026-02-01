package org.example.homedatazip.apartment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.global.geocode.dto.CoordinateInfoResponse;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final ApartmentTradeSaleSaveService apartmentSaveService;
    private final GeoService geoService;

    // 매매
    @Transactional
    public Map<String, Apartment> getOrCreateApartmentsFromTradeSale(List<ApartmentTradeSaleItem> items) {
        List<String> aptSeqs = items.stream().map(ApartmentTradeSaleItem::getAptSeq).distinct().toList();

        // 기존 DB 데이터 로드
        Map<String, Apartment> aptMap = apartmentRepository.findAllByAptSeqIn(aptSeqs)
                .stream()
                .collect(Collectors.toMap(Apartment::getAptSeq, a -> a));

        for (ApartmentTradeSaleItem item : items) {
            String seq = item.getAptSeq();
            if (!aptMap.containsKey(seq)) {
                CoordinateInfoResponse response = geoService.convertCoordinateInfo(
                        item.getAptDong(), item.getJibun(), item.getSggCd(), item.getAptNm(),
                        item.getRoadNm(), item.getRoadNmBonbun(), item.getRoadNmBubun()
                );

                if (response == null) continue;

                // 별도 트랜잭션에서 안전하게 저장 시도
                Apartment apt = apartmentSaveService.saveAndGetApartment(item, response);
                if (apt != null) {
                    aptMap.put(seq, apt);
                }
            } else {
                aptMap.get(seq).update(item);
            }
        }
        return aptMap;
    }

    // 전월세
}
