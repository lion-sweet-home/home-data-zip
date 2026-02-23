package org.example.homedatazip.global.batch.tradeRent.Filter;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TradeRentRegionFilter {
    private TradeRentRegionFilter() {}

    private static final Set<String> ALLOWED_SIDO_PREFIX = Set.of("11");

    public static boolean isAllowedBySggcd(String sggCode) {
        if (sggCode == null) return false;
        String code = sggCode.trim();
        if (code.length() < 2) return false;
        return ALLOWED_SIDO_PREFIX.contains(code.substring(0, 2));
    }

}
