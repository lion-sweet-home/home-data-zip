package org.example.homedatazip.recommend.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.recommend.dto.ApartmentResponse;
import org.example.homedatazip.recommend.dto.QApartmentResponse;
import org.example.homedatazip.recommend.dto.UserPreference;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.example.homedatazip.apartment.entity.QApartment.apartment;
import static org.example.homedatazip.data.QRegion.region;
import static org.example.homedatazip.monthAvg.entity.QMonthAvg.monthAvg;
import static org.example.homedatazip.recommend.entity.QUserSearchLog.userSearchLog;


@Repository
@RequiredArgsConstructor
public class ApartmentRecommendationRepository {

    private final JPAQueryFactory queryFactory;

    public List<ApartmentResponse> findRecommendedApartments(UserPreference pref, Long userId) {
        long minPrice = (long) (pref.preferredPrice() * 0.6);
        long maxPrice = (long) (pref.preferredPrice() * 1.4);
        double minArea = pref.preferredArea() * 0.7;
        double maxArea = pref.preferredArea() * 1.3;

        NumberExpression<Long> avgPrice;
        NumberExpression<Long> avgMonthly = Expressions.asNumber(0L);
        BooleanExpression typeCountCondition;
        String currentTradeType;
        long areaModulo;

        if (!pref.isRent()) {
            avgPrice = monthAvg.saleDealAmountSum.divide(monthAvg.saleCount.when(0).then(1).otherwise(monthAvg.saleCount)).longValue();
            typeCountCondition = monthAvg.saleCount.gt(0);
            currentTradeType = "SALE";
            areaModulo = 10000000L;
        } else if (pref.isWolse()) {
            avgPrice = monthAvg.wolseDepositSum.divide(monthAvg.wolseCount.when(0).then(1).otherwise(monthAvg.wolseCount)).longValue();
            avgMonthly = monthAvg.wolseRentSum.divide(monthAvg.wolseCount.when(0).then(1).otherwise(monthAvg.wolseCount)).longValue();
            typeCountCondition = monthAvg.wolseCount.gt(0);
            currentTradeType = "WOLSE";
            areaModulo = 1000000L;
        } else {
            avgPrice = monthAvg.jeonseDepositSum.divide(monthAvg.jeonseCount.when(0).then(1).otherwise(monthAvg.jeonseCount)).longValue();
            typeCountCondition = monthAvg.jeonseCount.gt(0);
            currentTradeType = "RENT";
            areaModulo = 1000000L;
        }

        NumberExpression<Double> extractedArea = Expressions.numberTemplate(Double.class,
                "({0} % " + areaModulo + ") / 100.0", monthAvg.areaTypeId);

        BooleanExpression wolseFilter = null;
        if (pref.isWolse()) {
            wolseFilter = monthAvg.wolseRentSum.gt(0);
        }

        return queryFactory
                .select(new QApartmentResponse(
                        apartment.id,
                        apartment.aptName,
                        apartment.roadAddress,
                        apartment.jibunAddress,
                        apartment.buildYear,
                        apartment.latitude,
                        apartment.longitude,
                        Expressions.stringTemplate("concat({0}, ' ', {1}, ' ', {2})",
                                region.sido, region.gugun, region.dong),
                        extractedArea,
                        avgPrice.max(),
                        avgMonthly.max(),
                        Expressions.asString(currentTradeType)
                ))
                .from(apartment)
                .join(apartment.region, region)
                .join(monthAvg).on(apartment.id.eq(monthAvg.aptId))
                .leftJoin(userSearchLog).on(
                        userSearchLog.userId.eq(userId),
                        userSearchLog.sggCode.eq(region.sggCode),
                        userSearchLog.createdAt.gt(LocalDateTime.now().minusDays(30))
                )
                .where(
                        region.sggCode.eq(pref.favoriteSggCode()),
                        typeCountCondition,
                        avgPrice.between(minPrice, maxPrice),
                        extractedArea.between(minArea, maxArea),
                        pref.isWolse() ? avgMonthly.between(pref.preferredMonthly() * 0.5, pref.preferredMonthly() * 1.5) : null
                )
                .groupBy(
                        apartment.id, apartment.aptName, apartment.roadAddress, apartment.jibunAddress,
                        apartment.buildYear, apartment.latitude, apartment.longitude,
                        region.sido, region.gugun, region.dong, monthAvg.areaTypeId
                )
                .orderBy(
                        userSearchLog.score.sum().desc().nullsLast(),
                        monthAvg.yyyymm.max().desc(),
                        avgPrice.max().asc()
                )
                .limit(6)
                .fetch();
    }

    public List<ApartmentResponse> findDefaultRecommendations() {
        return queryFactory
                .select(new QApartmentResponse(
                        apartment.id,
                        apartment.aptName,
                        apartment.roadAddress,
                        apartment.jibunAddress,
                        apartment.buildYear,
                        apartment.latitude,
                        apartment.longitude,
                        Expressions.stringTemplate("concat({0}, ' ', {1}, ' ', {2})",
                                region.sido, region.gugun, region.dong),
                        Expressions.asNumber(0.0),
                        Expressions.asNumber(0L),
                        Expressions.asNumber(0L),
                        Expressions.asString("SALE")
                ))
                .from(apartment)
                .join(apartment.region, region)
                .join(monthAvg).on(apartment.id.eq(monthAvg.aptId))
                .orderBy(monthAvg.saleCount.desc())
                .limit(6)
                .fetch();
    }
}