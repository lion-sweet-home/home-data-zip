package org.example.homedatazip.school.dto;

import java.util.List;

/** 학교 지역 검색 요청 (시도·구군 필수, 동·schoolLevel optional) */
public record SchoolSearchRequest(
        String sido,
        String gugun,
        String dong,
        List<String> schoolLevel
) {}
