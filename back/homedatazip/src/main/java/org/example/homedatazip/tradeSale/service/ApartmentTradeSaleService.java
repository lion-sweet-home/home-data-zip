package org.example.homedatazip.tradeSale.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.service.ApartmentService;
import org.example.homedatazip.monthAvg.service.MonthAvgRebuildService;
import org.example.homedatazip.tradeSale.Repository.ApartmentTradeSaleRepository;
import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;
import org.example.homedatazip.tradeSale.entity.TradeSale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApartmentTradeSaleService {

    private final ApartmentService apartmentService;
    private final ApartmentTradeSaleRepository apartmentTradeSaleRepository;
    private final MonthAvgRebuildService monthAvgRebuildService;

    @Transactional
    public void processChunk(List<ApartmentTradeSaleItem> items) {
        // 아파트 정보
        Map<String, Apartment> apartmentMap = apartmentService.getOrCreateApartmentsFromTradeSale(items);

        // 아파트 정보가 존재하는 아이템만 TradeSale로 변환
        List<TradeSale> tradeSales = items.stream()
                .filter(item -> apartmentMap.containsKey(item.getAptSeq())) // 아파트가 성공적으로 매칭된 것만!
                .map(item -> TradeSale.from(item, apartmentMap.get(item.getAptSeq())))
                .toList();

        if (!tradeSales.isEmpty()) {
            apartmentTradeSaleRepository.saveAll(tradeSales);
            monthAvgRebuildService.rebuildSaleFor(tradeSales);

        }
    }
}
