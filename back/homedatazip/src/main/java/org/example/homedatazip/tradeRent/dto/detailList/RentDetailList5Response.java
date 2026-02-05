package org.example.homedatazip.tradeRent.dto.detailList;

import org.example.homedatazip.tradeRent.entity.TradeRent;

import java.util.List;

public record RentDetailList5Response(
        Long aptId,
        Long areaKey10,
        Double exclusive,
        List<RentFromAptAreaResponse> items
) {

    public static RentDetailList5Response from(List<TradeRent> tradeRents, long aptId, long areaKey10) {
        return new RentDetailList5Response(
                aptId,
                areaKey10,
                toExclusive(areaKey10),
                tradeRents.stream().map(RentFromAptAreaResponse::from).toList()
        );
    }

    /** 0.1㎡ 키(예: 421) -> 42.1㎡ */
    private static double toExclusive(long areaKey10) {
        return areaKey10 / 100.0;
    }
}
