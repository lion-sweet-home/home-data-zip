package org.example.homedatazip.monthAvg.repository.impl;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.QApartment;
import org.example.homedatazip.data.QRegion;
import org.example.homedatazip.monthAvg.dto.JeonseCountResponse;
import org.example.homedatazip.monthAvg.dto.WolseCountResponse;
import org.example.homedatazip.monthAvg.entity.QMonthAvg;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MonthAvgDSLRepositoryImpl implements MonthAvgDSLRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<JeonseCountResponse> getMonthCount(String sido, String gugun, int period) {

        YearMonth periodyyyymm = YearMonth.now().minusMonths(period-1);
        String yyyymm = periodyyyymm.format(DateTimeFormatter.ofPattern("yyyyMM"));

        QMonthAvg monthAvg = QMonthAvg.monthAvg;
        QRegion region = QRegion.region;
        QApartment apartment = QApartment.apartment;

        NumberExpression<Long> jeonseCountSum = monthAvg.jeonseCount.sum().longValue();

        return queryFactory
                .select(
                        Projections.constructor(
                        JeonseCountResponse.class,
                        region.sido,
                        region.gugun,
                        region.dong,
                        jeonseCountSum
                ))
                .from(monthAvg)
                .join(apartment).on(apartment.id.eq(monthAvg.aptId))
                .join(region).on(region.eq(apartment.region))
                .where(
                        region.sido.eq(sido),
                        region.gugun.eq(gugun),
                        monthAvg.yyyymm.goe(yyyymm)
                )
                .groupBy(region.sido, region.gugun, region.dong)
                .fetch();
    }

    @Override
    public List<WolseCountResponse> getMonthWolseCount(String sido, String gugun, int period) {
        YearMonth periodyyyymm = YearMonth.now().minusMonths(period-1);
        String yyyymm = periodyyyymm.format(DateTimeFormatter.ofPattern("yyyyMM"));

        QMonthAvg monthAvg = QMonthAvg.monthAvg;
        QRegion region = QRegion.region;
        QApartment apartment = QApartment.apartment;

        NumberExpression<Long> wolseCountSum = monthAvg.wolseCount.sum().longValue();

        return queryFactory
                .select(
                        Projections.constructor(
                                WolseCountResponse.class,
                                region.sido,
                                region.gugun,
                                region.dong,
                                wolseCountSum
                        ))
                .from(monthAvg)
                .join(apartment).on(apartment.id.eq(monthAvg.aptId))
                .join(region).on(region.eq(apartment.region))
                .where(
                        region.sido.eq(sido),
                        region.gugun.eq(gugun),
                        monthAvg.yyyymm.goe(yyyymm)
                )
                .groupBy(region.sido, region.gugun, region.dong)
                .fetch();
    }
}
