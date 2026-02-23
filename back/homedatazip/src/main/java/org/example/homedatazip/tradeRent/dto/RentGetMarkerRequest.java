package org.example.homedatazip.tradeRent.dto;

public record RentGetMarkerRequest (
        String sido,
        String gugun,
        String dong,
        Long minDeposit,
        Long maxDeposit,
        Integer minMonthlyRent,
        Integer maxMonthlyRent,
        Double minExclusive,
        Double maxExclusive,
        Integer level,
        Integer limit,
        Double east,
        Double west,
        Double north,
        Double south
){}
