package org.example.homedatazip.monthAvg.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.monthAvg.dto.*;
import org.example.homedatazip.monthAvg.entity.MonthAvg;
import org.example.homedatazip.monthAvg.repository.MonthAvgRepository;
import org.example.homedatazip.monthAvg.repository.impl.JeonseMonthAvgTop3Repository;
import org.example.homedatazip.monthAvg.repository.impl.MonthAvgDSLRepository;
import org.example.homedatazip.monthAvg.repository.impl.WolseMonthAvgTop3Repository;
import org.example.homedatazip.monthAvg.utill.Yyyymm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthAvgRentService {

    private final MonthAvgRepository monthAvgRepository;
    private final JeonseMonthAvgTop3Repository jeonseMonthAvgTop3Repository;
    private final WolseMonthAvgTop3Repository wolseMonthAvgTop3Repository;
    private final MonthAvgDSLRepository monthAvgDSLRepository;

    // 아파트의 월별 거래량 첫 페이지 그래프
    @Transactional(readOnly = true)
    public List<MonthTotalTradeResponse> getTotalTrade(Long aptId, int period){
        period = period == 0 ? 6 : period;

        YearMonth periodyyyymm = YearMonth.now().minusMonths(period);
        String yyyymm = periodyyyymm.format(DateTimeFormatter.ofPattern("yyyyMM"));

        LocalDate today = LocalDate.now();

        String maxYyyymm = Yyyymm.lastMonthYyyymm(today); //현재 Month -1
        String minYyyymm = Yyyymm.minYyyymmForMonths(maxYyyymm, period); // 현재 Month -1 ~ -7 총 6달

        return monthAvgRepository.findMonthlyTotals(aptId,minYyyymm, maxYyyymm);
    }

    // 구까지만 조회 했을 때 나타나는 기간별 카운트
    //전세 리팩토링 완료
    @Transactional(readOnly = true)
    public List<JeonseCountResponse>  getJeonseCountByGu(String si, String gu, int period){
        period = period == 0 ? 6 : period;

        return monthAvgDSLRepository.getMonthCount(si, gu, period);
    }
    //월세
    @Transactional(readOnly = true)
    public List<WolseCountResponse>  getWolseCountByGu(String si, String gu, int period){
        period = period == 0 ? 6 : period;


        return monthAvgDSLRepository.getMonthWolseCount(si, gu, period);
    }

    //평수별, 월별 평균값 데이터 호출 점표현
    @Transactional(readOnly = true)
    public List<RentDetailAvg> getRentAreaTypeAvg(long aptId, long areaKey, int period){
        Long areaTypeId = aptId * 1_000_000L  + areaKey;

        String maxYyyymm = Yyyymm.lastMonthYyyymm(LocalDate.now());
        String minYyyymm = Yyyymm.minYyyymmForMonths(maxYyyymm, period);

        List<MonthAvg> monthAvgs =
                monthAvgRepository.findAllByAptIdAndAreaTypeIdAndYyyymmBetweenOrderByYyyymmAsc(aptId, areaTypeId, minYyyymm, maxYyyymm);

        return monthAvgs.stream().map(RentDetailAvg::from).toList();

    }

    //상세보기 카드에 area filter로 적용된 응답dto
    @Transactional(readOnly = true)
    public List<MonthTotalTradeAreaResponse> getTotalTradeWithExclusive(Long aptId, Integer periodRow, Long areaKey){



        Long areaTypeId = aptId * 1_000_000L  + areaKey;

        String maxYyyymm = Yyyymm.lastMonthYyyymm(LocalDate.now());
        String minYyyymm = Yyyymm.minYyyymmForMonths(maxYyyymm, periodRow);

        List<MonthAvg> monthAvgs =
                monthAvgRepository.findAllByAptIdAndAreaTypeIdAndYyyymmBetweenOrderByYyyymmAsc(aptId, areaTypeId, minYyyymm, maxYyyymm);

        return monthAvgs.stream()
                .map(MonthTotalTradeAreaResponse::from)
                .toList();
    }

    //아파트 detail page에서 평수 list 카드 데이터 호출
    @Transactional(readOnly = true)
    public AreaTypeResponse getAptAreaKey(Long aptId){
        List<Long> areaTypeIds = monthAvgRepository.findDistinctAreaTypeIdsByAptId(aptId);

        return AreaTypeResponse.map(areaTypeIds, aptId);
    }

    //면적카드 선택시 나오는 최신 평균 거래가 (6개월)
    @Transactional(readOnly = true)
    public AreaTypeMonthAvgResponse getMonthsAvg(Long aptId, Long areaKey){

        Long areaTypeId = aptId * 1_000_000L  + areaKey;

        String maxYyyymm = Yyyymm.lastMonthYyyymm(LocalDate.now());
        String minYyyymm = Yyyymm.minYyyymmForMonths(maxYyyymm, 6);

        List<MonthAvg> monthAvgs =
                monthAvgRepository.findAllByAptIdAndAreaTypeIdAndYyyymmBetweenOrderByYyyymmAsc(aptId, areaTypeId, minYyyymm, maxYyyymm);

        return AreaTypeMonthAvgResponse.map(monthAvgs);
    }



    //아파트, 구, 평균가, 거래가 등락률
    @Transactional(readOnly = true)
    public List<MonthTop3JeonsePriceResponse> getJeonseTop3ByRegion(){
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        return jeonseMonthAvgTop3Repository.top3ByLastMonth(lastMonth);
    }

    @Transactional(readOnly = true)
    public List<MonthTop3WolsePriceResponse> getWolseTop3ByRegion(){
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        return wolseMonthAvgTop3Repository.top3RentByLastMonth(lastMonth);
    }
}
