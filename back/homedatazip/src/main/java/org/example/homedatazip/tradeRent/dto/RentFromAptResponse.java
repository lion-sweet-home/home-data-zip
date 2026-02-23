package org.example.homedatazip.tradeRent.dto;

import org.example.homedatazip.tradeRent.entity.TradeRent;

import java.time.LocalDate;

public record RentFromAptResponse(
        Long id,

        Long apartmentId,
        String apartmentName,

        Long deposit,
        Integer monthlyRent,

        Integer floor,
        Double exclusiveArea,
        LocalDate dealDate
){
    public static RentFromAptResponse from(TradeRent tradeRent){
        return new RentFromAptResponse(
                tradeRent.getId(),

                tradeRent.getApartment().getId(),
                tradeRent.getApartment().getAptName(),

                tradeRent.getDeposit(),
                tradeRent.getMonthlyRent(),

                tradeRent.getFloor(),
                tradeRent.getExclusiveArea(),
                tradeRent.getDealDate()
        );
    }
}

