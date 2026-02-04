package org.example.homedatazip.monthAvg.service;

import lombok.RequiredArgsConstructor;

import org.example.homedatazip.monthAvg.repository.*;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.example.homedatazip.tradeSale.entity.TradeSale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MonthAvgRebuildService {

    private final MonthAvgRepository monthAvgRepository;
    private final TradeRentAggRepository tradeRentAggRepository;
    private final TradeSaleAggRepository tradeSaleAggRepository;

    private static final DateTimeFormatter YYYYMM = DateTimeFormatter.ofPattern("yyyyMM");

    @Transactional
    public void rebuildRentFor(List<TradeRent> touchedRents){
        Set<Long> aptIds = new HashSet<>();
        Set<String> yyyymms = new HashSet<>();

        for (TradeRent tradeRent : touchedRents){
            aptIds.add(tradeRent.getApartment().getId());
            yyyymms.add(tradeRent.getDealDate().format(YYYYMM));
        }
        if(aptIds.isEmpty() ||  yyyymms.isEmpty()) return;

        List<TradeRentAggRow> rows = tradeRentAggRepository.aggregateByAptMonthAndArea(aptIds, yyyymms);

        for (TradeRentAggRow row : rows){
            monthAvgRepository.upsertReplaceRent(
                    row.getAptId(),
                    row.getYyyymm(),
                    row.getAreaTypeId(),
                    row.getJeonseDepositSum(),
                    row.getWolseDepositSum(),
                    row.getWolseRentSum(),
                    row.getJeonseCount(),
                    row.getWolseCount()
            );
        }
    }
    @Transactional
    public void rebuildSaleFor(List<TradeSale> touchedRents){
        Set<Long> aptIds = new HashSet<>();
        Set<String> yyyymms = new HashSet<>();

        for (TradeSale tradeSale : touchedRents){
            aptIds.add(tradeSale.getApartment().getId());
            yyyymms.add(tradeSale.getDealDate().format(YYYYMM));
        }
        if(aptIds.isEmpty() ||  yyyymms.isEmpty()) return;

        List<TradeSaleAggRow> rows = tradeSaleAggRepository.aggregateByAptMonthAndArea(aptIds, yyyymms);

        for (TradeSaleAggRow row : rows){
            monthAvgRepository.upsertReplaceSale(
                    row.getAptId(),
                    row.getYyyymm(),
                    row.getAreaTypeId(),
                    row.getSaleDealAmountSum(),
                    row.getSaleCount()
            );
        }
    }
}
