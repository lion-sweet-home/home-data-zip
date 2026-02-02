package org.example.homedatazip.apartment.dto;

public record MarkResponse(
        Long aptId,
        String aptNm,
        Double latitude,
        Double longitude
) {}
