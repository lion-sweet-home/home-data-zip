package org.example.homedatazip.apartment.repository.rentDSL;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;

import java.util.List;

import static org.example.homedatazip.apartment.entity.QApartment.apartment;
import static org.example.homedatazip.tradeRent.entity.QTradeRent.tradeRent;

@RequiredArgsConstructor
public class ApartmentRepositoryImpl implements ApartmentRepositoryCustomRent {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Apartment> findAllWithRentByRegionAndRentRange(RentGetMarkerRequest request) {
        BooleanBuilder where = new BooleanBuilder()
                .and(eqSido(request.sido()))
                .and(eqGugun(request.gugun()))
                .and(startsWithDong(request.dong()));

        BooleanBuilder rentWhere = new BooleanBuilder()
                .and(minDeposit(request.minDeposit()))
                .and(maxDeposit(request.maxDeposit()))
                .and(minMonthlyRent(request.minMonthlyRent()))
                .and(maxMonthlyRent(request.maxMonthlyRent()));


        return queryFactory
                .select(apartment)
                .from(apartment)
                .where(where)
                .where(
                        queryFactory.selectOne()
                                .from(tradeRent)
                                .where(tradeRent.apartment.eq(apartment))
                                .where(rentWhere)
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
}
