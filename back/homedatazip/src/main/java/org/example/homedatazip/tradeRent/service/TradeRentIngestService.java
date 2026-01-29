package org.example.homedatazip.tradeRent.service;

import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.data.RegionRepository;
import org.example.homedatazip.tradeRent.api.MolitRentClient;
import org.example.homedatazip.tradeRent.repository.TradeRentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneId;

@Service
public class TradeRentIngestService {

    private final RegionRepository regionRepository;
    private final TradeRentRepository tradeRentRepository;
    private final ApartmentRepository apartmentRepository;
    private final MolitRentClient molitRentClient;

    public TradeRentIngestService(RegionRepository regionRepository,
                                  TradeRentRepository tradeRentRepository,
                                  ApartmentRepository apartmentRepository,
                                  MolitRentClient molitRentClient) {
        this.regionRepository = regionRepository;
        this.tradeRentRepository = tradeRentRepository;
        this.apartmentRepository = apartmentRepository;
        this.molitRentClient = molitRentClient;
    }

    public String lastMonthYmd(){
        YearMonth ym = YearMonth.now(ZoneId.of("Asia/Seoul")).minusMonths(1);
        return "%04d%02d".formatted(ym.getYear(),ym.getMonthValue());
    }

    @Transactional
    public IngestResult(){}



    private IngestResult
}
