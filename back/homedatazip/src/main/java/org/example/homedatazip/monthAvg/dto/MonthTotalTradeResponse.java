package org.example.homedatazip.monthAvg.dto;

import org.example.homedatazip.monthAvg.entity.MonthAvg;

import java.util.List;

public record MonthTotalTradeResponse(
        Long aptId,
        String yyyymm,
        Long jeonseCount,
        Long wolseCount
){ }

