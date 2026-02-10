package org.example.homedatazip.monthAvg.repository.impl;

import com.querydsl.core.annotations.QueryProjection;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.QApartment;
import org.example.homedatazip.data.QRegion;
import org.example.homedatazip.monthAvg.dto.MonthTop3WolsePriceResponse;
import org.example.homedatazip.monthAvg.entity.QMonthAvg;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WolseMonthAvgTop3RepositoryImpl implements WolseMonthAvgTop3Repository {

    private final JPAQueryFactory queryFactory;
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");


    public record AptMonthAggRow(
            Long areaTypeId,
            Long aptId,
            Double exclusive,
            String aptName,
            String gugun,
            Integer countSum,
            Long depositSum,
            Long monthRentSum
    ) {}

    public record PrevAggRow(
            Long areaTypeId,
            Integer countSum,
            Long depositSum,
            Long monthRentSum
    ) {}

    @Override
    public List<MonthTop3WolsePriceResponse> top3RentByLastMonth(YearMonth lastMonth) {

        String lastYm = lastMonth.format(YM_FMT);
        String prevYm = lastMonth.minusMonths(1).format(YM_FMT);

        QMonthAvg m = QMonthAvg.monthAvg;
        QApartment a = QApartment.apartment;
        QRegion r = QRegion.region;


        NumberExpression<Long> exclusive100 = Expressions.numberTemplate(
                Long.class,
                "MOD({0}, 1000000)",
                m.areaTypeId
        );
        NumberExpression<Double> exclusiveVal = exclusive100.doubleValue().divide(100.0);


        NumberExpression<Integer> lastCountSum = m.wolseCount.sum();
        NumberExpression<Long> lastDepositSum = m.wolseDepositSum.sum();
        NumberExpression<Long> lastMonthRentSum = m.wolseRentSum.sum();

        List<AptMonthAggRow> lastTop3 = queryFactory
                .select(Projections.constructor(
                        AptMonthAggRow.class,
                        m.areaTypeId,
                        m.aptId,
                        exclusiveVal,
                        a.aptName,
                        r.gugun,
                        lastCountSum,
                        lastDepositSum,
                        lastMonthRentSum
                ))
                .from(m)
                .join(a).on(a.id.eq(m.aptId))
                .join(r).on(r.id.eq(a.region.id))
                .where(m.yyyymm.eq(lastYm))
                .groupBy(m.areaTypeId, m.aptId, a.aptName, r.gugun)
                .orderBy(lastCountSum.desc(), m.areaTypeId.asc())
                .limit(3)
                .fetch();

        if (lastTop3.isEmpty()) return List.of();

        List<Long> topAreaTypeIds = lastTop3.stream().map(AptMonthAggRow::areaTypeId).toList();

        NumberExpression<Integer> prevCountSum = m.wolseCount.sum();
        NumberExpression<Long> prevDepositSum = m.wolseDepositSum.sum();
        NumberExpression<Long> prevMonthRentSum = m.wolseRentSum.sum();

        List<PrevAggRow> prevAggRows = queryFactory
                .select(Projections.constructor(
                        PrevAggRow.class,
                        m.areaTypeId,
                        prevCountSum,
                        prevDepositSum,
                        prevMonthRentSum
                ))
                .from(m)
                .where(m.yyyymm.eq(prevYm), m.areaTypeId.in(topAreaTypeIds))
                .groupBy(m.areaTypeId)
                .fetch();

        Map<Long, PrevAggRow> prevByAreaTypeId = prevAggRows.stream()
                .collect(Collectors.toMap(PrevAggRow::areaTypeId, x -> x));

        List<MonthTop3WolsePriceResponse> result = new ArrayList<>();

        for (AptMonthAggRow last : lastTop3) {
            PrevAggRow prev = prevByAreaTypeId.get(last.areaTypeId);


            Long lastAvgDeposit = safeAvg(last.depositSum, last.countSum);
            Long prevAvgDeposit = (prev == null) ? null : safeAvg(prev.depositSum, prev.countSum);
            Double depositRate = safeChangeRate(lastAvgDeposit, prevAvgDeposit);


            Long lastAvgRent = safeAvg(last.monthRentSum, last.countSum);
            Long prevAvgRent = (prev == null) ? null : safeAvg(prev.monthRentSum, prev.countSum);
            Double rentRate = safeChangeRate(lastAvgRent, prevAvgRent);

            result.add(new MonthTop3WolsePriceResponse(
                    last.aptId,
                    last.exclusive,
                    last.aptName,
                    last.gugun,
                    lastYm,
                    last.countSum,
                    lastAvgDeposit,
                    depositRate,
                    lastAvgRent,
                    rentRate
            ));
        }

        return result;
    }

    private Long safeAvg(Long sum, Integer count) {
        if (sum == null || count == null || count == 0) return null;
        return sum / count; // ✅ 내림
    }

    private Double safeChangeRate(Long lastAvg, Long prevAvg) {
        if (lastAvg == null || prevAvg == null || prevAvg == 0L) return null;
        return ((double) (lastAvg - prevAvg) / (double) prevAvg) * 100.0;
    }
}