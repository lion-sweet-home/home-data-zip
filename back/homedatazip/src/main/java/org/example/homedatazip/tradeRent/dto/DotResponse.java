package org.example.homedatazip.tradeRent.dto;

import java.time.LocalDate;

public record DotResponse(
        Long deposit,
        Integer mothlyRent,
        String yyyymm
) {}
