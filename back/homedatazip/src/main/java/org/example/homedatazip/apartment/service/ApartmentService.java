package org.example.homedatazip.apartment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.dto.AptSaleAggregation;
import org.example.homedatazip.apartment.dto.AptSummaryResponse;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.apartment.repository.ApartmentSearchRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.ApartmentErrorCode;
import org.example.homedatazip.global.geocode.dto.CoordinateInfoResponse;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.example.homedatazip.monthAvg.utill.Yyyymm;
import org.example.homedatazip.tradeRent.dto.ApartmentGetOrCreateRequest;
import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private final ApartmentSearchRepository apartmentSearchRepository;

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
                        item.getUmdNm(), item.getJibun(), item.getSggCd(), item.getAptNm(),
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

        List<String> aptSeqs = new ArrayList<>(reqByAptSeq.keySet());
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
                CoordinateInfoResponse response = geoService.convertCoordinateInfo(
                        req.umdNm(), req.jibun(),req.sggCd(),req.aptName(),
                        req.roadNm(),req.roadBonBun(),req.roadBonBun());
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
                newApartments.add(newApt);
            }else{
                found.updateByRent(req);
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

    /**
     * 키워드 검색
     * <br/>
     * 1. 키워드 유효성 검증
     * 2. 키워드로 시작하는 아파트 목록 조회
     * 3. 아파트 ID 추출 및 기간 설정
     * 4. 집계 데이터 조회
     * 5. 응답 DTO 생성
     */
    public List<AptSummaryResponse> searchByKeyword(String keyword) {
        // 1. 키워드 유효성 검증
        validateKeyword(keyword);

        log.info("🔍 아파트 키워드 검색 시작 - keyword: {}", keyword);

        // 2. 키워드를 포함하는 아파트 목록 조회
        List<Apartment> apartments
                = apartmentRepository.findByAptNameContaining(keyword);

        // 조회 결과 없음
        if (apartments == null || apartments.isEmpty()) {
            log.info("❌ 키워드 검색 결과 없음 - keyword: {}", keyword);
            return null; // 프론트 쪽에서 검색 결과 없다고 표기
        }

        log.info("🏠 아파트 조회 완료 - keyword: {}, 검색된 아파트: {}건",
                keyword,
                apartments.size()
        );

        // 3. 아파트 ID 추출 및 기간 설정
        List<Long> aptIds = apartments.stream()
                .map(Apartment::getId)
                .toList();

        // 전월, 전전월, 6개월 전
        String lastMonth = Yyyymm.lastMonthYyyymm(LocalDate.now());
        String twoMonthsAgo = Yyyymm.minYyyymmForMonths(lastMonth, 2);
        String sixMonthsAgo = Yyyymm.minYyyymmForMonths(lastMonth, 6);

        log.debug("📅 조회기간 - 전월: {}, 전전월: {}, 6개월 전: {}",
                lastMonth,
                twoMonthsAgo,
                sixMonthsAgo
        );

        // 4. 집계 데이터 조회
        Map<Long, AptSaleAggregation> aggregationMap = apartmentSearchRepository
                .findSaleAggregationByAptIds(
                        aptIds,
                        sixMonthsAgo,
                        twoMonthsAgo,
                        lastMonth
                )
                .stream()
                .collect(Collectors.toMap(
                                AptSaleAggregation::aptId,
                                aggregation -> aggregation
                        )
                );

        // 5. 응답 DTO 생성
        List<AptSummaryResponse> result = apartments.stream()
                .map(apt -> createSummaryResponse(
                                apt,
                                aggregationMap.get(apt.getId())
                        )
                )
                .toList();

        log.info("✅ 아파트 키워드 검색 완료 - keyword: {}, 응답: {}건",
                keyword,
                result.size()
        );

        return result;
    }

    /**
     * 키워드 유효성 검증
     */
    private void validateKeyword(String keyword) {
        // 공백 체크
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ApartmentErrorCode.KEYWORD_CANNOT_BLANK);
        }
        // 글자수 체크
        if (keyword.trim().length() < 2) {
            throw new BusinessException(ApartmentErrorCode.INVALID_KEYWORD_LENGTH);
        }
    }

    /**
     * 응답 DTO 생성
     */
    private AptSummaryResponse createSummaryResponse(
            Apartment apt,
            AptSaleAggregation aggregation
    ) {
        Long aptId = apt.getId();
        String gu = (apt.getRegion() != null)
                ? apt.getRegion().getGugun()
                : null;

        // 집계 데이터가 없는 경우
        if (aggregation == null) {
            log.debug("⚠️ 거래 데이터 없음 - aptId: {}, aptName: {}",
                    aptId,
                    apt.getAptName()
            );

            return new AptSummaryResponse(
                    aptId,
                    apt.getAptName(),
                    gu,
                    null,
                    null,
                    null
            );
        }

        // 집계 데이터가 있는 경우
        Long avgDealAmount = aggregation.getSixMonthAvgAmount();
        Integer tradeCount
                = Optional.ofNullable(aggregation.sixMonthSaleCount())
                .map(Long::intValue)
                .orElse(null);

        Double priceChangeRate
                = calculatePriceChangeRate(aptId, apt.getAptName(), aggregation);

        return new AptSummaryResponse(
                aptId,
                apt.getAptName(),
                gu,
                avgDealAmount,
                priceChangeRate,
                tradeCount
        );
    }

    /**
     * 등락률 계산
     * <br/>
     * (전월 평균 거래가 - 전전월 평균 거래가) / 전전월 평균 거래가 * 100
     */
    private Double calculatePriceChangeRate(
            Long aptId,
            String aptName,
            AptSaleAggregation aggregation
    ) {
        if (
                aggregation.twoMonthsAgoAmountSum() == null
                        || aggregation.twoMonthsAgoSaleCount() == 0
        ) {
            log.debug("⚠️ 등락률 계산 불가(전전월 거래 없음) - aptId: {}, aptName: {}",
                    aptId,
                    aptName
            );
            return null;
        }

        if (
                aggregation.lastMonthAmountSum() == null
                        || aggregation.lastMonthSaleCount() == 0
        ) {
            log.debug("⚠️ 등락률 계산 불가(전월 거래 없음) - aptId: {}, aptName: {}",
                    aptId,
                    aptName
            );
            return null;
        }

        return aggregation.getPriceChangeRate();
    }
}
