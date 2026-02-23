package org.example.homedatazip.tradeSale.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.apartment.dto.MarkerClusterResponse;
import org.example.homedatazip.apartment.dto.QMarkResponse;
import org.example.homedatazip.tradeSale.dto.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static org.example.homedatazip.apartment.entity.QApartment.apartment;
import static org.example.homedatazip.data.QRegion.region;
import static org.example.homedatazip.tradeSale.entity.QTradeSale.tradeSale;

@Repository
@RequiredArgsConstructor
public class TradeSaleQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<MarkResponse> searchMarkerByRegion(SaleSearchRequest request) {
        long safeLimit = normalizeLimit(request.limit());

        com.querydsl.jpa.impl.JPAQuery<MarkResponse> query = queryFactory
                .select(new QMarkResponse(
                        apartment.id,
                        apartment.aptName,
                        apartment.latitude,
                        apartment.longitude
                ))
                .from(tradeSale)
                .join(tradeSale.apartment, apartment)
                .join(apartment.region, region)
                .where(
                        containsSido(request.sido()),
                        containsGugun(request.gugun()),
                        containsDong(request.dong()),
                        amountBetween(request.minAmount(), request.maxAmount()),
                        areaBetween(request.minArea(), request.maxArea()),
                        buildYearBetween(request.minBuildYear(), request.maxBuildYear()),
                        periodBetween(request.periodMonths()),
                        latBetween(request.south(), request.north()),
                        lngBetween(request.west(), request.east()),
                        tradeSale.canceled.ne(true)
                )
                .groupBy(apartment.id);

        if (safeLimit > 0) {
            query.limit(safeLimit);
        }

        return query.fetch();
    }

    /**
     * bounds + level + (기존 필터) 기반으로 마커를 격자(grid) 단위로 묶어서 count를 반환한다.
     */
    public List<MarkerClusterResponse> searchMarkerClustersByRegion(SaleSearchRequest request) {
        double grid = gridByLevel(request.level());
        long safeLimit = normalizeClusterLimit(request.limit());

        NumberExpression<Long> latKey =
                Expressions.numberTemplate(Long.class, "FLOOR({0} / {1})", apartment.latitude, grid);
        NumberExpression<Long> lngKey =
                Expressions.numberTemplate(Long.class, "FLOOR({0} / {1})", apartment.longitude, grid);

        com.querydsl.jpa.impl.JPAQuery<MarkerClusterResponse> query = queryFactory
                .select(com.querydsl.core.types.Projections.constructor(
                        MarkerClusterResponse.class,
                        apartment.latitude.avg(),
                        apartment.longitude.avg(),
                        apartment.id.countDistinct()
                ))
                .from(tradeSale)
                .join(tradeSale.apartment, apartment)
                .join(apartment.region, region)
                .where(
                        containsSido(request.sido()),
                        containsGugun(request.gugun()),
                        containsDong(request.dong()),
                        amountBetween(request.minAmount(), request.maxAmount()),
                        areaBetween(request.minArea(), request.maxArea()),
                        buildYearBetween(request.minBuildYear(), request.maxBuildYear()),
                        periodBetween(request.periodMonths()),
                        latBetween(request.south(), request.north()),
                        lngBetween(request.west(), request.east()),
                        tradeSale.canceled.ne(true)
                )
                .groupBy(latKey, lngKey)
                .orderBy(apartment.id.countDistinct().desc());

        if (safeLimit > 0) {
            query.limit(safeLimit);
        }

        return query.fetch();
    }

    private BooleanExpression containsDong(String dong) {
        if (dong == null || dong.isBlank()) return null;
        return region.dong.contains(dong.trim());
    }

    private BooleanExpression containsSido(String sido) {
        if (sido == null || sido.isBlank()) return null;
        return region.sido.contains(sido.trim());
    }

    private BooleanExpression containsGugun(String gugun) {
        if (gugun == null || gugun.isBlank()) return null;
        return region.gugun.contains(gugun.trim());
    }

    // 기간 필터 생성 메서드
    private BooleanExpression periodBetween(Integer months) {
        // months가 null이거나 0이면 '전체보기'이므로 조건을 걸지 않음(null 반환)
        if (months == null || months <= 0) {
            return null;
        }

        // 현재 시점으로부터 n개월 전 날짜 계산
        LocalDate startDate = LocalDate.now().minusMonths(months);
        return tradeSale.dealDate.goe(startDate);
    }

    // 가격 범위 동적 쿼리 생성 메서드
    private BooleanExpression amountBetween(Long minAmount, Long maxAmount) {
        if (minAmount == null && maxAmount == null) return null; // 필터 없으면 전체 조회

        if (minAmount != null && maxAmount != null) {
            return tradeSale.dealAmount.between(minAmount, maxAmount);
        } else if (minAmount != null) {
            return tradeSale.dealAmount.goe(minAmount); // min 이상
        } else {
            return tradeSale.dealAmount.loe(maxAmount); // max 이하
        }
    }

    // 면적 범위 동적 쿼리 생성 메서드
    private BooleanExpression areaBetween(Double minArea, Double maxArea) {
        if (minArea == null && maxArea == null) return null;

        // minArea와 maxArea가 모두 있을 때 .between() 사용
        if (minArea != null && maxArea != null) {
            return tradeSale.exclusiveArea.between(minArea, maxArea);
        }
        // 하나만 있을 때 처리
        if (minArea != null) return tradeSale.exclusiveArea.goe(minArea);
        return tradeSale.exclusiveArea.loe(maxArea);
    }

    // 건축 일자 범위 동적 쿼리 생성 메서드
    private BooleanExpression buildYearBetween(Integer minYear, Integer maxYear) {
        if (minYear == null && maxYear == null) return null;
        if (minYear != null && maxYear != null) return apartment.buildYear.between(minYear, maxYear);
        if (minYear != null) return apartment.buildYear.goe(minYear);
        return apartment.buildYear.loe(maxYear);
    }

    private BooleanExpression latBetween(Double south, Double north) {
        if (!isFinite(south) || !isFinite(north)) return null;
        double min = Math.min(south, north);
        double max = Math.max(south, north);
        return apartment.latitude.between(min, max);
    }

    private BooleanExpression lngBetween(Double west, Double east) {
        if (!isFinite(west) || !isFinite(east)) return null;
        double min = Math.min(west, east);
        double max = Math.max(west, east);
        return apartment.longitude.between(min, max);
    }

    private boolean isFinite(Double v) {
        return v != null && !v.isNaN() && !v.isInfinite();
    }

    private long normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) return 0;
        return Math.min(limit, 50_000);
    }

    private double gridByLevel(Integer level) {
        int lv = (level == null) ? 8 : level;
        if (lv >= 12) return 0.05;
        if (lv >= 10) return 0.02;
        if (lv >= 8)  return 0.01;
        if (lv >= 6)  return 0.005;
        if (lv >= 4)  return 0.002;
        return 0.001;
    }

    private long normalizeClusterLimit(Integer limit) {
        if (limit == null || limit <= 0) return 5_000;
        return Math.min(limit, 20_000);
    }

    public List<RecentTradeSale> findRecentTrades(Long aptId) {
        return queryFactory
                .select(new QRecentTradeSale(
                        tradeSale.dealAmount,
                        tradeSale.exclusiveArea,
                        tradeSale.dealDate.stringValue(),
                        tradeSale.floor
                ))
                .from(tradeSale)
                .where(
                        tradeSale.apartment.id.eq(aptId),
                        tradeSale.canceled.ne(true)
                )
                .orderBy(tradeSale.dealDate.desc())
                .limit(5)
                .fetch();
    }

    // 상세 거래 내역
    public List<TradeSaleHistory> findTradeHistory(Long aptId) {

        NumberExpression<Double> areaKey = tradeSale.exclusiveArea.multiply(100).round().divide(100.0);

        NumberExpression<Long> areaTypeId = tradeSale.apartment.id.multiply(10000000L)
                .add(tradeSale.exclusiveArea.multiply(100).castToNum(Long.class));

        return queryFactory
                .select(new QTradeSaleHistory(
                        tradeSale.dealAmount,
                        apartment.buildYear.stringValue(),
                        tradeSale.floor,
                        tradeSale.dealDate.stringValue(),
                        tradeSale.exclusiveArea,
                        areaKey,
                        areaTypeId
                ))
                .from(tradeSale)
                .join(tradeSale.apartment, apartment)
                .where(tradeSale.apartment.id.eq(aptId))
                .orderBy(tradeSale.dealDate.desc())
                .fetch();
    }

    public List<TradeVolumeDto> countMonthlyTrades(Long aptId, int monthsToView) {
        LocalDate startDate = LocalDate.now().minusMonths(monthsToView);

        StringExpression formattedDate = (StringExpression) Expressions.stringTemplate(
                "DATE_FORMAT({0}, '%Y%m')",
                tradeSale.dealDate
        );

        return queryFactory
                .select(new QTradeVolumeDto(
                        formattedDate,
                        tradeSale.count()
                ))
                .from(tradeSale)
                .where(
                        tradeSale.apartment.id.eq(aptId),
                        tradeSale.dealDate.goe(startDate),
                        tradeSale.canceled.ne(true)
                )
                .groupBy(formattedDate)
                .orderBy(formattedDate.asc())
                .fetch();
    }

    public List<DongRankResponse> findDongRankByRegion(String sido, String gugun, int periodMonths) {
        LocalDate startDate = LocalDate.now().minusMonths(periodMonths);

        return queryFactory
                .select(new QDongRankResponse(
                        region.dong,
                        tradeSale.count()
                ))
                .from(tradeSale)
                .join(tradeSale.apartment, apartment)
                .join(apartment.region, region)
                .where(
                        region.sido.eq(sido),
                        region.gugun.eq(gugun),
                        tradeSale.dealDate.goe(startDate),
                        tradeSale.canceled.ne(true)
                )
                .groupBy(region.dong)
                .orderBy(tradeSale.count().desc())
                .fetch();
    }

}