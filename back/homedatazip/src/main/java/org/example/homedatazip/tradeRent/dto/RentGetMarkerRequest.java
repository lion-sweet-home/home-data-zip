package org.example.homedatazip.tradeRent.dto;

public record RentGetMarkerRequest (
        String sido,
        String gugun,
        String dong,
        Long minDeposit,
        Long maxDeposit,
        Integer minMonthlyRent,
        Integer maxMonthlyRent
){}
