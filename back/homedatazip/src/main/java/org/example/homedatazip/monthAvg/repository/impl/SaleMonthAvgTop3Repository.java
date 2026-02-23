package org.example.homedatazip.monthAvg.repository.impl;

import org.example.homedatazip.monthAvg.dto.MonthTop3SalePriceResponse;

import java.time.YearMonth;
import java.util.List;

public interface SaleMonthAvgTop3Repository {
    List<MonthTop3SalePriceResponse> top3ByLastMonth(YearMonth lastMonth);
}

