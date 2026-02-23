package org.example.homedatazip.apartment.repository.rentDSL;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.apartment.dto.MarkerClusterResponse;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;

import java.util.List;

import static org.example.homedatazip.apartment.entity.QApartment.apartment;
import static org.example.homedatazip.tradeRent.entity.QTradeRent.tradeRent;

@RequiredArgsConstructor
public class ApartmentRepositoryImpl implements ApartmentRepositoryCustomRent {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MarkResponse> findAllWithRentByRegionAndRentRange(RentGetMarkerRequest request) {
        BooleanBuilder where = new BooleanBuilder()
                .and(eqSido(request.sido()))
                .and(eqGugun(request.gugun()))
                .and(startsWithDong(request.dong()));

        BooleanBuilder rentWhere = new BooleanBuilder()
                .and(minDeposit(request.minDeposit()))
                .and(maxDeposit(request.maxDeposit()))
                .and(minMonthlyRent(request.minMonthlyRent()))
                .and(maxMonthlyRent(request.maxMonthlyRent()))
                .and(minExclusive(request.minExclusive()))
                .and(maxExclusive(request.maxExclusive()));

        BooleanBuilder bounds = new BooleanBuilder()
                .and(latBetween(request.south(), request.north()))
                .and(lngBetween(request.west(), request.east()));

        long safeLimit = normalizeLimit(request.limit());

        com.querydsl.jpa.impl.JPAQuery<MarkResponse> query = queryFactory
                .select(Projections.constructor(
                        MarkResponse.class,
                        apartment.id,
                        apartment.aptName,
                        apartment.latitude,
                        apartment.longitude
                ))
                .from(apartment)
                .where(where)
                .where(
                        queryFactory.selectOne()
                                .from(tradeRent)
                                .where(tradeRent.apartment.eq(apartment),
                                        rentWhere)
                                .exists()
                )
                .where(bounds);

        if (safeLimit > 0) {
            query.limit(safeLimit);
        }

        return query.fetch();
    }

    @Override
    public List<MarkerClusterResponse> findRentMarkerClusters(RentGetMarkerRequest request) {
        // 기존 region + rent 필터 유지 (원하면: bounds가 있을 때 region 필터를 빼는 정책으로 바꿀 수 있음)
        BooleanBuilder where = new BooleanBuilder()
                .and(eqSido(request.sido()))
                .and(eqGugun(request.gugun()))
                .and(startsWithDong(request.dong()));

        BooleanBuilder rentWhere = new BooleanBuilder()
                .and(minDeposit(request.minDeposit()))
                .and(maxDeposit(request.maxDeposit()))
                .and(minMonthlyRent(request.minMonthlyRent()))
                .and(maxMonthlyRent(request.maxMonthlyRent()))
                .and(minExclusive(request.minExclusive()))
                .and(maxExclusive(request.maxExclusive()));

        BooleanBuilder bounds = new BooleanBuilder()
                .and(latBetween(request.south(), request.north()))
                .and(lngBetween(request.west(), request.east()));

        double grid = gridByLevel(request.level());
        long safeLimit = normalizeClusterLimit(request.limit());

        // MySQL/MariaDB: FLOOR(lat / grid), FLOOR(lng / grid)로 버킷 키 생성
        NumberExpression<Long> latKey =
                Expressions.numberTemplate(Long.class, "FLOOR({0} / {1})", apartment.latitude, grid);
        NumberExpression<Long> lngKey =
                Expressions.numberTemplate(Long.class, "FLOOR({0} / {1})", apartment.longitude, grid);

        com.querydsl.jpa.impl.JPAQuery<MarkerClusterResponse> query = queryFactory
                .select(Projections.constructor(
                        MarkerClusterResponse.class,
                        apartment.latitude.avg(),
                        apartment.longitude.avg(),
                        apartment.id.countDistinct()
                ))
                .from(apartment)
                .where(where)
                .where(
                        queryFactory.selectOne()
                                .from(tradeRent)
                                .where(tradeRent.apartment.eq(apartment), rentWhere)
                                .exists()
                )
                .where(bounds)
                .groupBy(latKey, lngKey)
                .orderBy(apartment.id.countDistinct().desc());

        if (safeLimit > 0) {
            query.limit(safeLimit);
        }

        return query.fetch();
    }
    private static BooleanExpression eqSido(String sido) {
        if (sido == null || sido.isBlank()) return null;
        return apartment.region.sido.eq(sido.trim());
    }

    private static BooleanExpression eqGugun(String gugun) {
        if (gugun == null || gugun.isBlank()) return null;
        return apartment.region.gugun.eq(gugun.trim());
    }

    private static BooleanExpression startsWithDong(String dongPrefix) {
        if (dongPrefix == null || dongPrefix.isBlank()) return null;
        return apartment.region.dong.startsWith(dongPrefix.trim());
    }

    private static BooleanExpression minDeposit(Long v) {
        return v == null ? null : tradeRent.deposit.goe(v);
    }

    private static BooleanExpression maxDeposit(Long v) {
        return v == null ? null : tradeRent.deposit.loe(v);
    }

    private static BooleanExpression minMonthlyRent(Integer v) {
        return v == null ? null : tradeRent.monthlyRent.goe(v);
    }

    private static BooleanExpression maxMonthlyRent(Integer v) {
        return v == null ? null : tradeRent.monthlyRent.loe(v);
    }
    private static BooleanExpression minExclusive(Double v) {
        return v == null ? null : tradeRent.exclusiveArea.goe(v);
    }
    private static BooleanExpression maxExclusive(Double v) {
        return v == null ? null : tradeRent.exclusiveArea.loe(v);
    }

    private static BooleanExpression latBetween(Double south, Double north) {
        if (!isFinite(south) || !isFinite(north)) return null;
        double min = Math.min(south, north);
        double max = Math.max(south, north);
        return apartment.latitude.between(min, max);
    }

    private static BooleanExpression lngBetween(Double west, Double east) {
        if (!isFinite(west) || !isFinite(east)) return null;
        double min = Math.min(west, east);
        double max = Math.max(west, east);
        return apartment.longitude.between(min, max);
    }

    private static boolean isFinite(Double v) {
        return v != null && !v.isNaN() && !v.isInfinite();
    }

    /**
     * limit이 null/0/음수면 "미적용"으로 간주한다.
     * (마커 API는 기본적으로 bounds로 잘리므로, 필요할 때만 옵션으로 limit을 건다)
     */
    private static long normalizeLimit(Integer limit) {
        if (limit == null) return 0;
        if (limit <= 0) return 0;
        // 안전장치: 실수로 너무 큰 limit이 들어오는 걸 방지
        return Math.min(limit, 50_000);
    }

    /**
     * 줌 레벨에 따라 (deg 단위) 격자 크기를 결정한다.
     * - level이 클수록 더 넓은 범위를 보므로 grid도 크게 해서 더 강하게 묶는다.
     *
     * 참고: Kakao map level(1~14)은 1이 가장 확대, 숫자가 커질수록 축척이 커진다.
     */
    private static double gridByLevel(Integer level) {
        int lv = (level == null) ? 8 : level;
        if (lv >= 12) return 0.05;   // ~ 수 km 단위
        if (lv >= 10) return 0.02;
        if (lv >= 8)  return 0.01;
        if (lv >= 6)  return 0.005;
        if (lv >= 4)  return 0.002;
        return 0.001;                // 매우 확대: 거의 개별에 가깝게
    }

    /**
     * 클러스터 결과는 너무 커지면 응답/렌더링이 무거워지므로 기본 limit을 둔다.
     * - null/0/음수면 기본값 적용
     */
    private static long normalizeClusterLimit(Integer limit) {
        if (limit == null || limit <= 0) return 5_000;
        return Math.min(limit, 20_000);
    }
}
