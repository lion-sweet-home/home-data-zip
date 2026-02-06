package org.example.homedatazip.monthAvg.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.monthAvg.dto.*;
import org.example.homedatazip.monthAvg.entity.MonthAvg;
import org.example.homedatazip.monthAvg.repository.MonthAvgRepository;
import org.example.homedatazip.monthAvg.utill.Yyyymm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthAvgRentService {

    private final MonthAvgRepository monthAvgRepository;

    // 아파트의 월별 거래량 첫 페이지 그래프
    @Transactional(readOnly = true)
    public List<MonthTotalTradeResponse> getTotalTrade(Long aptId, String periodRow){

        PeriodOption period = PeriodOption.fromNullable(periodRow);

        LocalDate today = LocalDate.now();

        String maxYyyymm = Yyyymm.lastMonthYyyymm(today); //현재 Month -1
        String minYyyymm = Yyyymm.minYyyymmForMonths(maxYyyymm, period.months()); // 현재 Month -1 ~ -7 총 6달

        return monthAvgRepository.findMonthlyTotals(aptId,minYyyymm, maxYyyymm);
    }

    //상세보기 카드에 area filter로 적용된 응답dto
    @Transactional(readOnly = true)
    public List<MonthTotalTradeAreaResponse> getTotalTradeWithExclusive(Long aptId, String periodRow, Long areaKey){

        PeriodOption period = PeriodOption.fromNullable(periodRow);


        Long areaTypeId = aptId * 1_000_000L  + areaKey;

        String maxYyyymm = Yyyymm.lastMonthYyyymm(LocalDate.now());
        String minYyyymm = Yyyymm.minYyyymmForMonths(maxYyyymm, period.months());

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

    //평수별, 월별 평균값 데이터 호출
    @Transactional(readOnly = true)
    public List<RentDetailAvg> getRentAreaTypeAvg(long aptId, long areaKey, String periodRow){
        PeriodOption period = PeriodOption.fromNullable(periodRow);

        Long areaTypeId = aptId * 1_000_000L  + areaKey;

        String maxYyyymm = Yyyymm.lastMonthYyyymm(LocalDate.now());
        String minYyyymm = Yyyymm.minYyyymmForMonths(maxYyyymm, period.months());

        List<MonthAvg> monthAvgs =
                monthAvgRepository.findAllByAptIdAndAreaTypeIdAndYyyymmBetweenOrderByYyyymmAsc(aptId, areaTypeId, minYyyymm, maxYyyymm);
        return monthAvgs.stream().map(RentDetailAvg::from).toList();

    }
}
