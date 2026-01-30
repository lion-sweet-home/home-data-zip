package org.example.homedatazip.global.batch.tradeRent.processor;

import org.example.homedatazip.tradeRent.dto.MolitRentApiItemResponse;
import org.example.homedatazip.tradeRent.dto.TradeRentWriteRequest;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;

public class TradeProcessor implements ItemProcessor<MolitRentApiItemResponse, TradeRentWriteRequest> {
    @Override
    public TradeRentWriteRequest process(MolitRentApiItemResponse item) throws Exception {
        String sggCd = norm(item.sggCd());
        String aptSeq = norm(item.aptSeq());
        String umdNm = norm(item.umdNm());
        String jibun = norm(item.jibun());
        String jibunKey = (umdNm != null && jibun != null) ? (umdNm + " " + jibun) : null;

        Long deposit = parseLong(item.deposit());
        Integer monthlyRent = parseInt(item.monthlyRent());
        Double exclusiveArea = parseDouble(item.excluUseAr());
        Integer floor = parseInt(item.floor());

        Integer y = parseInt(item.dealYear());
        Integer m = parseInt(item.dealMonth());
        Integer d = parseInt(item.dealDay());
        LocalDate dealDate = (y != null && m != null && d != null) ? LocalDate.of(y, m, d) : null;

        if (sggCd == null || dealDate == null || deposit == null || monthlyRent == null || exclusiveArea == null || floor == null) {
            return null;
        }

        return new TradeRentWriteRequest(
                sggCd,
                aptSeq,
                umdNm,
                jibun,
                jibunKey,
                dealDate,
                deposit,
                monthlyRent,
                exclusiveArea,
                floor,
                norm(item.contractTerm()),
                norm(item.contractType()),
                norm(item.useRRRight())
        );
    }
    private static String norm(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    private static String cleanNum(String raw) {
        String v = norm(raw);
        if (v == null) return null;
        v = v.replace(",", "").trim();
        return (v.isEmpty() || "-".equals(v)) ? null : v;
    }
    private static Long parseLong(String raw) {
        String c = cleanNum(raw);
        if (c == null) return null;
        try { return Long.parseLong(c); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseInt(String raw) {
        String c = cleanNum(raw);
        if (c == null) return null;
        try { return Integer.parseInt(c); } catch (NumberFormatException e) { return null; }
    }

    private static Double parseDouble(String raw) {
        String c = cleanNum(raw);
        if (c == null) return null;
        try { return Double.parseDouble(c); } catch (NumberFormatException e) { return null; }
    }
}