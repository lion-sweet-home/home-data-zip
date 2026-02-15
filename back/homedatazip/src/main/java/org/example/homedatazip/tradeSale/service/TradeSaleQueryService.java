package org.example.homedatazip.tradeSale.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.monthAvg.entity.MonthAvg;
import org.example.homedatazip.monthAvg.repository.MonthAvgRepository;
import org.example.homedatazip.monthAvg.utill.Yyyymm;
import org.example.homedatazip.tradeSale.repository.TradeSaleQueryRepository;
import org.example.homedatazip.tradeSale.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeSaleQueryService {

    private final TradeSaleQueryRepository tradeSaleQueryRepository;
    private final ApartmentRepository apartmentRepository;
    private final MonthAvgRepository monthAvgRepository;

    public List<DongRankResponse> getDongRanking(String sido, String gugun, int periodMonths) {
        // 만약 시/도나 구/군이 없으면 빈 리스트 반환
        if (sido == null || gugun == null) {
            return Collections.emptyList();
        }
        return tradeSaleQueryRepository.findDongRankByRegion(sido, gugun, periodMonths);
    }


    // 마커 조회
    public List<MarkResponse> getMarkers(SaleSearchRequest request) {

        return tradeSaleQueryRepository.searchMarkerByRegion(request);
    }

    // 아파트 요약
    public AptSaleSummaryResponse getAptSaleSummary(Long aptId, Integer periodMonths) {

        int monthsToView = (periodMonths != null) ? periodMonths : 6;

        // 아파트 조회
        Apartment apt = apartmentRepository.findById(aptId)
                .orElseThrow(() -> new IllegalArgumentException("해당 아파트가 존재하지 않습니다."));

        // 최근 거래 내역 5건 조회
        List<RecentTradeSale> recentTradeSales = tradeSaleQueryRepository.findRecentTrades(aptId);

        // 보여줄 전체 달 리스트 생성
        List<String> monthLabels = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = monthsToView - 1; i >= 0; i--) {
            monthLabels.add(now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM")));
        }

        // DB에서 실제 거래가 있었던 데이터 조회
        List<TradeVolumeDto> dbVolumes = tradeSaleQueryRepository.countMonthlyTrades(aptId, monthsToView);

        // 빠른 조회를 위해 Map으로 변환
        Map<String, Long> volumeMap = dbVolumes.stream()
                .collect(Collectors.toMap(TradeVolumeDto::month, TradeVolumeDto::count));

        // 생성한 monthLabels를 순회
        List<Long> monthlyVolumes = monthLabels.stream()
                .map(label -> volumeMap.getOrDefault(label, 0L))
                .collect(Collectors.toList());

        return new AptSaleSummaryResponse(
                apt.getAptName(),
                monthlyVolumes,
                recentTradeSales,
                monthLabels,
                apt.getBuildYear()
        );
    }

    // 아파트 상세 보기
    public AptDetailResponse getAptDetail(Long aptId, Integer periodMonths) {

        Apartment apt = apartmentRepository.findById(aptId)
                .orElseThrow(() -> new IllegalArgumentException("아파트 정보를 찾을 수 없습니다."));

        // 거래 히스토리 조회
        List<TradeSaleHistory> histories = tradeSaleQueryRepository.findTradeHistory(aptId);

        int monthsToView = (periodMonths != null) ? periodMonths : 6;
        String minDateLine = LocalDate.now().minusMonths(monthsToView)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Map<Double, List<TradeSaleHistory>> pyeongTrades = histories.stream()
                .filter(h -> h.dealDate().compareTo(minDateLine) >= 0 || monthsToView == 0)
                .collect(Collectors.groupingBy(TradeSaleHistory::areaKey));

        // 최신 평균가 계산
        Long latestAvgAmount = calculateLatestAvgAmount(aptId);

        // 차트 데이터 생성 로직 호출
        Map<Double, List<TradeSaleChartData>> pyeongChartDataMap =
                generateChartDataMap(aptId, periodMonths, pyeongTrades);

        return new AptDetailResponse(
                apt.getAptName(),
                apt.getRoadAddress(),
                latestAvgAmount,
                pyeongTrades,
                pyeongChartDataMap,
                apt.getBuildYear()
        );
    }

    // 차트 데이터만 별도 조회
    public AptChartResponse getAptChartOnly(Long aptId, Integer periodMonths) {
        List<TradeSaleHistory> histories = tradeSaleQueryRepository.findTradeHistory(aptId);
        Map<Double, List<TradeSaleHistory>> pyeongTrades = histories.stream()
                .collect(Collectors.groupingBy(TradeSaleHistory::areaKey));

        Map<Double, List<TradeSaleChartData>> chartDataMap = generateChartDataMap(aptId, periodMonths, pyeongTrades);

        return new AptChartResponse(chartDataMap);
    }

    // 실제 차트 Map을 만드는 메서드
    private Map<Double, List<TradeSaleChartData>> generateChartDataMap(Long aptId, Integer periodMonths, Map<Double, List<TradeSaleHistory>> pyeongTrades) {
        int monthsToView = (periodMonths != null) ? periodMonths : 6;

        // 조회할 전체 기간 리스트 생성
        List<String> allMonths = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = monthsToView - 1; i >= 0; i--) {
            allMonths.add(now.minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM")));
        }

        String minYyyymm = allMonths.get(0);
        String maxYyyymm = allMonths.get(allMonths.size() - 1);

        return pyeongTrades.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {

                            Map<String, List<IndividualTrade>> tradeDotsMap = entry.getValue().stream()
                                    .collect(Collectors.groupingBy(
                                            h -> {
                                                String pureDate = h.dealDate().replace("-", "");
                                                return pureDate.substring(0, 6);
                                            },
                                            Collectors.mapping(h -> new IndividualTrade(
                                                    h.dealDate(),
                                                    h.dealAmount(),
                                                    h.floor()
                                            ), Collectors.toList())
                                    ));

                            Long targetAreaTypeId = entry.getValue().get(0).areaTypeId();

                            // DB 데이터 조회
                            List<MonthAvg> stats = monthAvgRepository.findAllByAptIdAndAreaTypeIdAndYyyymmBetweenOrderByYyyymmAsc(
                                    aptId, targetAreaTypeId, minYyyymm, maxYyyymm
                            );

                            Optional<MonthAvg> lastRecordBeforeStart = monthAvgRepository.findTopByAptIdAndAreaTypeIdAndYyyymmBeforeOrderByYyyymmDesc(
                                    aptId, targetAreaTypeId, minYyyymm
                            );

                            // 빠른 조회를 위해 Map으로 변환
                            Map<String, MonthAvg> statsMap = stats.stream()
                                    .collect(Collectors.toMap(MonthAvg::getYyyymm, s -> s));

                            List<TradeSaleChartData> filledData = new ArrayList<>();
                            long lastKnownAvg = lastRecordBeforeStart
                                    .map(m -> m.getSaleCount() > 0 ? m.getSaleDealAmountSum() / m.getSaleCount() : 0L)
                                    .orElse(0L);

                            // 모든 달을 순회하며 빈 곳 채우기
                            for (String month : allMonths) {
                                MonthAvg s = statsMap.get(month);

                                List<IndividualTrade> dots = tradeDotsMap.getOrDefault(month, new ArrayList<>());

                                if (s != null && s.getSaleCount() != null && s.getSaleCount() > 0) {
                                    // 데이터가 있는 경우: 새로운 평균가 갱신
                                    lastKnownAvg = s.getSaleDealAmountSum() / s.getSaleCount();
                                    filledData.add(new TradeSaleChartData(month, lastKnownAvg, (long) s.getSaleCount(),dots));
                                } else {
                                    // 데이터가 없는 경우: 거래량은 0, 평균가는 직전 값 유지
                                    filledData.add(new TradeSaleChartData(month, lastKnownAvg, 0L, dots));
                                }
                            }
                            return filledData;
                        }
                ));
    }

    // 최신 평균가 계산 메소드
    private Long calculateLatestAvgAmount(Long aptId) {
        String maxYyyymm = Yyyymm.lastMonthYyyymm(LocalDate.now());
        List<MonthAvg> latestStats = monthAvgRepository.findAllByAptIdAndYyyymm(aptId, maxYyyymm);

        if (latestStats.isEmpty()) {
            String realMaxYyyymm = monthAvgRepository.findTopByAptIdOrderByYyyymmDesc(aptId)
                    .map(MonthAvg::getYyyymm)
                    .orElse(maxYyyymm);
            latestStats = monthAvgRepository.findAllByAptIdAndYyyymm(aptId, realMaxYyyymm);
        }

        long totalCount = latestStats.stream().mapToLong(s -> s.getSaleCount() == null ? 0L : s.getSaleCount()).sum();
        long totalAmountSum = latestStats.stream().mapToLong(s -> s.getSaleDealAmountSum() == null ? 0L : s.getSaleDealAmountSum()).sum();

        return totalCount > 0 ? totalAmountSum / totalCount : 0L;
    }
}
