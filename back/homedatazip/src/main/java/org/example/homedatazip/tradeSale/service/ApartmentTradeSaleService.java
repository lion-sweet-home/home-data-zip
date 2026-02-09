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
        for (ApartmentTradeSaleItem item : items) {
            Apartment apt = apartmentMap.get(item.getAptSeq());
            if (apt == null) continue;

            // 데이터 전처리
            Long amount = Long.parseLong(item.getDealAmount().trim().replace(",", ""));
            java.time.LocalDate dealDate = java.time.LocalDate.of(
                    Integer.parseInt(item.getDealYear().trim()),
                    Integer.parseInt(item.getDealMonth().trim()),
                    Integer.parseInt(item.getDealDay().trim())
            );

            apartmentTradeSaleRepository.insertIgnore(
                    apt.getId(),
                    amount,
                    Double.parseDouble(item.getExcluUseAr().trim()),
                    Integer.parseInt(item.getFloor().trim()),
                    item.getAptDong(),
                    dealDate,
                    item.getSggCd(),
                    (item.getCdealType() != null && !item.getCdealType().isBlank())
            );
        }

        List<TradeSale> tradeSales = items.stream()
                .filter(item -> apartmentMap.containsKey(item.getAptSeq()))
                .map(item -> TradeSale.from(item, apartmentMap.get(item.getAptSeq())))
                .toList();

        if (!tradeSales.isEmpty()) {
            monthAvgRebuildService.rebuildSaleFor(tradeSales);

        }
    }
}
