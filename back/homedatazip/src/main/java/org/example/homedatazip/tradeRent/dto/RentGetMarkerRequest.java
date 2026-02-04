package org.example.homedatazip.tradeRent.dto;

import jakarta.persistence.Column;

public record RentGetMarkerRequest (
        String sido,
        String gugun,
        String dong,
        Long minDeposit,
        Long maxDeposit,
        Integer minMonthlyRent,
        Integer maxMonthlyRent
){}
