package org.example.homedatazip.tradeRent.dto;


public record DotResponse(
        Long deposit,
        Integer mothlyRent,
        String yyyymm,
        Integer floor
) {}
