package org.example.homedatazip.tradeRent.repository;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.tradeRent.dto.DotResponse;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.entity.QTradeRent;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TradeRentDSLRepositoryImpl implements TradeRentDSLRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<TradeRent> findItemsByArea(long aptId, long areaId, LocalDate date) {

        QTradeRent tradeRent = QTradeRent.tradeRent;

        double area = areaId /100.0;
        log.info("deal_date={}, area={} ", date, area);


        return queryFactory
                .select(tradeRent)
                .from(tradeRent)
                .where(tradeRent.exclusiveArea.eq(area),
                        tradeRent.apartment.id.eq(aptId),
                        tradeRent.dealDate.goe(date)
                )
                .fetch();
    }

    @Override
    public List<DotResponse> findDot(long aptId, LocalDate period) {

        log.info("deal_date={}, period={} ", period, period);

        QTradeRent tradeRent = QTradeRent.tradeRent;

        Expression<String> yyyymm =
                Expressions.stringTemplate("date_format({0}, '%Y%m')", tradeRent.dealDate);

        return queryFactory
                .select(Projections.constructor(
                        DotResponse.class,
                        tradeRent.deposit,
                        tradeRent.monthlyRent,
                        yyyymm
                ))
                .from(tradeRent)
                .where(
                        tradeRent.apartment.id.eq(aptId),
                        tradeRent.dealDate.goe(period)
                )
                .fetch();
    }

    @Override
    public List<TradeRent> findRecent5(long aptId) {

        QTradeRent tradeRent = QTradeRent.tradeRent;
        return queryFactory
                .select(tradeRent)
                .from(tradeRent)
                .where(tradeRent.apartment.id.eq(aptId))
                .limit(5)
                .fetch();
    }
}
