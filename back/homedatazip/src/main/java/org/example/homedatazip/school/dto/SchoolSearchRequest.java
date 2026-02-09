package org.example.homedatazip.school.dto;

/** 학교 지역 검색 요청 (시도·구군 필수, 동 optional) */
public record SchoolSearchRequest(
        String sido,
        String gugun,
        String dong
) {}
