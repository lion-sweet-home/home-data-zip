package org.example.homedatazip.tradeRent.dto;


public record DotResponse(
        Long deposit,
        Double exclusive,
        Integer mothlyRent,
        String yyyymm,
        Integer floor
) {}
