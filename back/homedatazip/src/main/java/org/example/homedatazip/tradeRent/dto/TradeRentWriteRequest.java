package org.example.homedatazip.tradeRent.dto;

import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;

import java.time.LocalDate;

public record TradeRentWriteRequest(
        String sggCd,
        String aptSeq,
        String umdNm,
        String jibun,      // "232" 같은 원문
        String jibunKey,   // "창신동 232"
        LocalDate dealDate,
        Long deposit,
        Integer monthlyRent,
        Double exclusiveArea,
        Integer floor,
        String contractTerm,
        String contractType,
        String useRRRight
) {
    public ApartmentGetOrCreateRequest toApartmentGetOrCreateRequest() {
        return ApartmentGetOrCreateRequest.from(this);
    }
}