package org.example.homedatazip.tradeRent.service;

import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.tradeRent.dto.RentApiItem;
import org.example.homedatazip.tradeRent.entity.TradeRent;

import java.time.LocalDate;

public class TradeRentMapperService {
    private TradeRentMapperService(){}

    public static TradeRent toEntity(Apartment apt, RentApiItem item){
        long deposit = parseLong(item.getDeposit());
        int monthlyRent = parseInt(item.getMonthlyRent());
        double exclusiveArea = parseDouble(item.getExcluUseAr());
        int floor = parseInt(item.getFloor());

        int y = parseInt(item.getDealYear());
        int m = parseInt(item.getDealMonth());
        int d = parseInt(item.getDealDay());

        LocalDate dealDate = LocalDate.of(y, m, d);
        Boolean renewalRequested = parseRenewal(item.getContractType(), item.getUseRRRight());

        String rentTerm = normalize(item.getContractTerm());
        if (rentTerm == null) rentTerm = "-";

        String sggCd = normalize(item.getSggCd());
        if (sggCd == null) throw new IllegalArgumentException("sggCd is required");

        return TradeRent.builder()
                .apartment(apt)
                .deposit(deposit)
                .monthlyRent(monthlyRent)
                .exclusiveArea(exclusiveArea)
                .floor(floor)
                .dealDate(dealDate)
                .renewalRequested(renewalRequested)
                .rentTerm(rentTerm)
                .sggCode(sggCd)
                .build();
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static long parseLong(String raw) {
        String c = cleanNumber(raw);
        if (c == null) return 0L;
        return Long.parseLong(c);
    }

    private static int parseInt(String raw) {
        String c = cleanNumber(raw);
        if (c == null) return 0;
        return Integer.parseInt(c);
    }

    private static double parseDouble(String raw) {
        String c = cleanNumber(raw);
        if (c == null) return 0.0;
        return Double.parseDouble(c);
    }

    private static String cleanNumber(String raw) {
        String v = normalize(raw);
        if (v == null) return null;
        v = v.replace(",", "").trim();
        if (v.isEmpty() || "-".equals(v)) return null;
        return v;
    }

    private static Boolean parseRenewal(String contractType, String useRRRight) {
        String ct = normalize(contractType);
        String rr = normalize(useRRRight);

        if (ct != null) {
            if (ct.contains("갱신")) return true;
            if (ct.contains("신규")) return false;
        }
        if (rr != null) {
            if (rr.contains("사용")) return true;
            if (rr.contains("미사용")) return false;
        }
        return null;
    }
}

