package org.example.homedatazip.monthAvg.repository.impl;

import org.example.homedatazip.monthAvg.dto.JeonseCountResponse;
import org.example.homedatazip.monthAvg.dto.WolseCountResponse;

import java.util.List;

public interface MonthAvgDSLRepository {
    List<JeonseCountResponse> getMonthCount(String sido, String gugun, int period);
    List<WolseCountResponse> getMonthWolseCount(String sido, String gugun, int period);
}
