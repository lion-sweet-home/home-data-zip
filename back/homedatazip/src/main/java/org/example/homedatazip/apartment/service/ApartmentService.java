package org.example.homedatazip.apartment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.global.geocode.dto.CoordinateInfoResponse;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.example.homedatazip.tradeRent.dto.ApartmentGetOrCreateRequest;
import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    public Map<String, Apartment> getOrCreateApartmentsFromTradeRent(List<ApartmentGetOrCreateRequest> items) {
        if (items == null || items.isEmpty()) {
            log.info("전월세 데이터 0건 처리 시도");
            return new HashMap<>();
        }

        log.info("전월세세 데이터 {}건 처리 시도", items.size());

        // null item 제거 + aptSeq null 제거 + aptSeq 기준으로 대표 1건만 남김 (중복 요청 방지)
        Map<String, ApartmentGetOrCreateRequest> reqByAptSeq = new HashMap<>();
        for (ApartmentGetOrCreateRequest it : items) {
            if (it == null) continue;
            if (it.aptSeq() == null || it.aptSeq().isBlank()) continue;
            reqByAptSeq.putIfAbsent(it.aptSeq(), it);
        }

        List<String> aptSeqs = new ArrayList<>();
        if (aptSeqs.isEmpty()) {
            log.warn("유효한 aptSeq가 없습니다. (items 내 aptSeq null/blank 가능)");
            return new HashMap<>();
        }

        // 기존 아파트 조회(repo가 null을 반환하는 이상 케이스까지 방어)
        List<Apartment> existing = Optional
                .ofNullable(apartmentRepository.findAllByAptSeqIn(aptSeqs))
                .orElseGet(List::of); //TODO

        Map<String, Apartment> aptMap = new HashMap<>(existing.size() * 2);
        for (Apartment a : existing) {
            if (a == null) continue;
            if (a.getAptSeq() == null || a.getAptSeq().isBlank()) continue; // toMap NPE 방지
            aptMap.put(a.getAptSeq(), a);
        }

        // 신규 아파트 리스트 생성
        List<Apartment> newApartments = new ArrayList<>();
        int geoSkipped = 0;

        // for문에서 신규 / 업데이트 판별
        for (ApartmentGetOrCreateRequest req : reqByAptSeq.values()) {
            Apartment found = aptMap.get(req.aptSeq());

            if (found == null) {
                if (req.umdNm() == null || req.umdNm().isBlank()) {
                    geoSkipped++;
                    log.warn("좌표 변환 스킵(필수값 누락) - aptSeq:{}, umdNm:{}, jibun:{}",
                            req.aptSeq(), req.umdNm(), req.jibun());
                    continue;
                }
                CoordinateInfoResponse response = geoService.convertCoordinateInfo(req.umdNm(), req.jibun());
                if (response == null) {
                    geoSkipped++;
                    log.warn("좌표 변환 스킵(응답 null) - aptSeq:{}, umdNm:{}, jibun:{}",
                            req.aptSeq(), req.umdNm(), req.jibun());
                    continue;
                }

                Apartment newApt = Apartment.createByRent(req,response);
                if (newApt == null) {
                    geoSkipped++;
                    log.warn("Apartment 생성 스킵(createByRent 결과 null) - aptSeq:{}", req.aptSeq());
                    continue;
                }
                aptMap.put(req.aptSeq(), newApt);
            }else{
                found.updateByRent(req);
            }

            if(!newApartments.isEmpty()){
                apartmentRepository.saveAll(newApartments);
            }


        }


        // 아파트 DB 저장
        if (!newApartments.isEmpty()) {
            apartmentRepository.saveAll(newApartments);
        }

        log.info("매매 데이터 처리 완료 - 신규 저장 {}건, 기존 데이터 활용 {}건",
                newApartments.size(), aptMap.size() - newApartments.size());

        return aptMap;
    }
}
