package org.example.homedatazip.monthAvg.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.monthAvg.dto.MonthTotalTradeResponse;
import org.example.homedatazip.monthAvg.dto.PeriodOption;
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

    // 아파트의 월별 거래량 첫 페이지
    @Transactional(readOnly = true)
    public MonthTotalTradeResponse getTotalTrade(Long aptId, String periodRow){

        PeriodOption period = PeriodOption.fromNullable(periodRow);

        LocalDate today = LocalDate.now();

        String maxYyyymm = Yyyymm.lastMonthYyyymm(today); //현재 Month -1
        String minYyyymm = Yyyymm.minYyyymmForMonths(maxYyyymm, period.months()); // 현재 Month -1 ~ -7 총 6달

        List<MonthAvg> allByYyyymmBetween = monthAvgRepository.findAllByAptIdAndYyyymmBetweenOrderByYyyymmAsc(aptId,minYyyymm, maxYyyymm);

        return MonthTotalTradeResponse.map(allByYyyymmBetween);
    }

    @Transactional(readOnly = true)
    public MonthTotalTradeResponse getTotalTradeWithExclusive(Long aptId, String periodRow, Double exclusive){

        PeriodOption period = PeriodOption.fromNullable(periodRow);

        Long areaKey = Math.round(exclusive * 10.0);
        Long areaTypeId = aptId * 1_000_000L  + areaKey;

        String maxYyyymm = Yyyymm.lastMonthYyyymm(LocalDate.now());
        String minYyyymm = Yyyymm.minYyyymmForMonths(maxYyyymm, period.months());

        List<MonthAvg> monthAvgs =
                monthAvgRepository.findAllByAptIdAndAreaTypeIdAndYyyymmBetweenOrderByYyyymmAsc(aptId, areaTypeId, minYyyymm, maxYyyymm);

        return MonthTotalTradeResponse.map(monthAvgs);
    }
}
