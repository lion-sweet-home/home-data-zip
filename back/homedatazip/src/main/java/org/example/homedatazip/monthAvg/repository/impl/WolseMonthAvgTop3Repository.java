package org.example.homedatazip.monthAvg.repository.impl;

import org.example.homedatazip.monthAvg.dto.MonthTop3WolsePriceResponse;

import java.time.YearMonth;
import java.util.List;

public interface WolseMonthAvgTop3Repository {
    List<MonthTop3WolsePriceResponse> top3RentByLastMonth(YearMonth lastMonth);
}
