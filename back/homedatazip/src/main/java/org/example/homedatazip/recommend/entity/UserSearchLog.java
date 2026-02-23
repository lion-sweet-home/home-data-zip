package org.example.homedatazip.recommend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.example.homedatazip.recommend.type.LogType;
import org.example.homedatazip.recommend.type.TradeType;


import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_search_log")
public class UserSearchLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String sggCode;
    private Long aptId;

    private Long minPrice;    // 최소 보증금/매매가
    private Long maxPrice;    // 최대 보증금/매매가
    private Long minMonthlyRent; // 추가: 최소 월세액 필터
    private Long maxMonthlyRent; // 추가: 최대 월세액 필터
    private Double minArea;
    private Double maxArea;
    private Integer buildYearFilter;
    private Integer periodMonths;

    private Double finalArea;
    private Long finalPrice;  // 매매가 / 전세보증금 / 월세보증금
    private Long monthlyRent;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;
    @Enumerated(EnumType.STRING)
    private LogType logType;
    private Integer score;
    private LocalDateTime createdAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserSearchLog(Long userId, Long aptId, String sggCode, Long minPrice, Long maxPrice,
                          Long minMonthlyRent, Long maxMonthlyRent, // 필터용 월세 추가
                          Double minArea, Double maxArea, Integer buildYearFilter,
                          Integer periodMonths, Double finalArea, Long finalPrice,
                          Long monthlyRent, // 확정 월세액 추가
                          TradeType tradeType, LogType logType) {
        this.userId = userId;
        this.aptId = aptId;
        this.sggCode = sggCode;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minMonthlyRent = (minMonthlyRent != null) ? minMonthlyRent : 0L;
        this.maxMonthlyRent = (maxMonthlyRent != null) ? maxMonthlyRent : 0L;
        this.minArea = minArea;
        this.maxArea = maxArea;
        this.buildYearFilter = buildYearFilter;
        this.periodMonths = periodMonths;
        this.finalArea = finalArea;
        this.finalPrice = finalPrice;
        this.monthlyRent = (monthlyRent != null) ? monthlyRent : 0L; // null 방지
        this.tradeType = tradeType;
        this.logType = logType;
        this.score = logType.getScore();
        this.createdAt = LocalDateTime.now();
    }

    // --- 정적 팩토리 메서드 ---

    // 1. 검색 로그 생성 (월세 필터 포함)
    public static UserSearchLog createSearchLog(Long userId, String sggCode, Long minPrice, Long maxPrice,
                                                Long minMonthlyRent, Long maxMonthlyRent,
                                                Double minArea, Double maxArea, Integer buildYear,
                                                Integer period, TradeType tradeType) {
        return UserSearchLog.builder()
                .userId(userId)
                .sggCode(sggCode)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minMonthlyRent(minMonthlyRent)
                .maxMonthlyRent(maxMonthlyRent)
                .minArea(minArea)
                .maxArea(maxArea)
                .buildYearFilter(buildYear)
                .periodMonths(period)
                .tradeType(tradeType)
                .logType(LogType.SEARCH)
                .build();
    }

    // 2. 액션 로그 (상세보기 등)
    public static UserSearchLog createActionLog(Long userId, String sggCode, TradeType tradeType, LogType logType) {
        return UserSearchLog.builder()
                .userId(userId)
                .sggCode(sggCode)
                .tradeType(tradeType)
                .logType(logType)
                .build();
    }

    // 3. 클릭 로그 (평수 클릭 시 - 보증금과 월세액 분리 저장)
    public static UserSearchLog createClickLog(Long userId, Long aptId, String sggCode, Double finalArea,
                                               Long finalPrice, Long monthlyRent, TradeType tradeType) {
        return UserSearchLog.builder()
                .userId(userId)
                .aptId(aptId)
                .sggCode(sggCode)
                .finalArea(finalArea)
                .finalPrice(finalPrice)
                .monthlyRent(monthlyRent)
                .tradeType(tradeType)
                .logType(LogType.PYEONG_CLICK)
                .build();
    }

    // 필터 업데이트 메서드
    public void updateFilters(Long minPrice, Long maxPrice, Long minMonthlyRent, Long maxMonthlyRent,
                              Double minArea, Double maxArea, Integer buildYear, Integer period) {
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minMonthlyRent = minMonthlyRent;
        this.maxMonthlyRent = maxMonthlyRent;
        this.minArea = minArea;
        this.maxArea = maxArea;
        this.buildYearFilter = buildYear;
        this.periodMonths = period;
    }
}