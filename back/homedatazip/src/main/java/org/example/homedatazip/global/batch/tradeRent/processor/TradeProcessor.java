package org.example.homedatazip.global.batch.tradeRent.processor;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.example.homedatazip.tradeRent.dto.RentApiItem;
import org.example.homedatazip.tradeRent.dto.TradeRentWriteRequest;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class TradeProcessor implements ItemProcessor<RentApiItem, TradeRentWriteRequest> {
    @Override
    public TradeRentWriteRequest process(RentApiItem item) throws Exception {
        String sggCd = norm(item.getSggCd());
        String aptSeq = norm(item.getAptSeq());
        String aptName = norm(item.getAptNm());
        String umdNm = norm(item.getUmdNm());
        String jibun = norm(item.getJibun());
        String jibunKey = (umdNm != null && jibun != null) ? (umdNm + " " + jibun) : null;

        String roadnm = norm(item.getRoadnm());
        String roadnmbonbun = norm(item.getRoadnmbonbun());
        String roadnmbubun = norm(item.getRoadnmbubun());

        Long deposit = parseLong(item.getDeposit());
        Integer monthlyRent = parseInt(item.getMonthlyRent());
        Double exclusiveArea = parseDouble(item.getExcluUseAr());
        Integer floor = parseInt(item.getFloor());

        Integer y = parseInt(item.getDealYear());
        Integer m = parseInt(item.getDealMonth());
        Integer d = parseInt(item.getDealDay());
        LocalDate dealDate = (y != null && m != null && d != null) ? LocalDate.of(y, m, d) : null;
        Integer buildYear = parseInt(item.getBuildYear());

        if (sggCd == null || dealDate == null || deposit == null || monthlyRent == null || exclusiveArea == null || floor == null) {
            return null;
        }

        return new TradeRentWriteRequest(
                sggCd,
                aptSeq,
                aptName,
                umdNm,
                jibun,
                jibunKey,
                roadnm,
                roadnmbonbun,
                roadnmbubun,
                dealDate,
                deposit,
                monthlyRent,
                exclusiveArea,
                floor,
                norm(item.getContractTerm()),
                norm(item.getContractType()),
                parseTriStateBoolean(item.getUseRRRight()),
                buildYear
        );
    }
    private static Boolean parseTriStateBoolean(String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isEmpty()) return null;

        // 기호/단일문자 케이스는 대소문자 영향 없게 먼저 정규화
        String lower = s.toLowerCase(Locale.ROOT);

        // true-ish (O/○ 포함)
        if (equalsAny(lower, "o", "ok", "○", "ㅇ") // 필요 없으면 "ㅇ" 제거
                || containsAny(lower, "갱신", "재계약", "연장", "요구", "청구", "사용")
                || equalsAny(lower, "y", "yes", "true", "1", "t")) {
            return true;
        }

        // false-ish (X/× 포함)
        if (equalsAny(lower, "x", "×", "✕", "✖") // 다양한 x 기호 대응
                || containsAny(lower, "신규", "미사용", "없음", "해당없음")
                || equalsAny(lower, "n", "no", "false", "0", "f")) {
            return false;
        }

        return null;
    }

    private static boolean equalsAny(String s, String... candidates) {
        for (String c : candidates) {
            if (s.equals(c)) return true;
        }
        return false;
    }

    private static boolean containsAny(String s, String... needles) {
        for (String n : needles) {
            if (s.contains(n)) return true;
        }
        return false;
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