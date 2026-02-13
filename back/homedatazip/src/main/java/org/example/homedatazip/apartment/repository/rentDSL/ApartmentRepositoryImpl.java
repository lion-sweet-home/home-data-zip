package org.example.homedatazip.apartment.repository.rentDSL;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;
import org.example.homedatazip.tradeRent.entity.QTradeRent;

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

        return queryFactory
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
                .fetch();
    }
    private static BooleanExpression eqSido(String sido) {
        return apartment.region.sido.eq(sido);
    }

    private static BooleanExpression eqGugun(String gugun) {
        return apartment.region.gugun.eq(gugun);
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
}
