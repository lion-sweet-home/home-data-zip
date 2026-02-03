package org.example.homedatazip.tradeRent.dto;

import org.example.homedatazip.tradeRent.entity.TradeRent;

import java.time.LocalDate;
import java.util.List;

public record RentFromAptResponse(
        List<RentResponse> rents
){

    public static RentFromAptResponse map(List<TradeRent> tradeRents){

        List<RentResponse> list = tradeRents.stream()
                .map(RentResponse::from)
                .toList();

        return new RentFromAptResponse(list);
    }

    public record RentResponse(
            Long id,

            Long apartmentId,
            String apartmentName,

            Long deposit,
            Integer monthlyRent,

            Integer floor,
            Double exclusiveArea,
            LocalDate dealDate
    ){
        public static RentResponse from(TradeRent tradeRent){
            return new RentResponse(
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
}
