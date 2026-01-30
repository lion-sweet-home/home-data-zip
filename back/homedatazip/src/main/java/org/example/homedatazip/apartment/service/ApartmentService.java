package org.example.homedatazip.apartment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.global.geocode.dto.CoordinateInfoResponse;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;
import org.springframework.stereotype.Service;
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
    private final GeoService geoService;

    // 매매
    public Map<String, Apartment> getOrCreateApartmentsFromTradeSale(List<ApartmentTradeSaleItem> items) {

        // 아파트 시퀀스 정보 가져오기 (중복제거)
        List<String> aptSeqs = items.stream().map(ApartmentTradeSaleItem::aptSeq).distinct().toList();

        // 기존 아파트 조회
        Map<String, Apartment> aptMap = apartmentRepository.findAllByAptSeqIn(aptSeqs)
                .stream()
                .collect(Collectors.toMap(Apartment::getAptSeq, apartment -> apartment));

        // 신규 아파트 리스트 생성
        List<Apartment> newApartments = new ArrayList<>();

        // for문에서 신규 / 업데이트 판별
        for (ApartmentTradeSaleItem item : items) {
            if (!aptMap.containsKey(item.aptSeq())) {
                // 지오코더 응답 결과
                CoordinateInfoResponse response = geoService.convertCoordinateInfo(item.aptDong(), item.jibun());

                // 아파트 엔티티 생성
                Apartment newApt = Apartment.create(item, response);

                // 기존 아파트 map에 저장, 신규 아파트 리스트에 저장
                aptMap.put(item.aptSeq(), newApt);
                newApartments.add(newApt);
            } else {
                // 업데이트
                aptMap.get(item.aptSeq()).update(item);
            }
        }

        // 아파트 DB 저장
        if (!newApartments.isEmpty()) {
            apartmentRepository.saveAll(newApartments);
        }

        return aptMap;
    }

    // 전월세
}
