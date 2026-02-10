package org.example.homedatazip.tradeSale.dto;

public record IndividualTrade(
        String date,    // 2025-03-08
        Long price,     // 260000
        Integer floor   // 6층 (툴팁용)
) {}
