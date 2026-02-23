package org.example.homedatazip.recommend.dto;

public record UserPyeongClickRequest(
        Long aptId,
        Double area,
        Long price,
        Long monthlyRent,
        boolean isRent
) {}
