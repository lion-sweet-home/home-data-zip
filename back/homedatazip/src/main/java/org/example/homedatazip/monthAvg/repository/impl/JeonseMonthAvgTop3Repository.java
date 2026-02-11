package org.example.homedatazip.monthAvg.repository.impl;

import org.example.homedatazip.monthAvg.dto.MonthTop3JeonsePriceResponse;

import java.time.YearMonth;
import java.util.List;

public interface JeonseMonthAvgTop3Repository {
    List<MonthTop3JeonsePriceResponse> top3ByLastMonth(YearMonth lastMonth);
}
