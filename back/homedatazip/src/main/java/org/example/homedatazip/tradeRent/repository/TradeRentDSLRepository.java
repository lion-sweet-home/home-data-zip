package org.example.homedatazip.tradeRent.repository;

import org.example.homedatazip.tradeRent.dto.DotResponse;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.entity.TradeRent;

import java.time.LocalDate;
import java.util.List;

public interface TradeRentDSLRepository {
    List<TradeRent> findItemsByArea(long areaId, long aptId, LocalDate date);
    List<DotResponse> findDot( long aptId, LocalDate date );
    List<TradeRent> findRecent5(long aptId);
        }
