package org.example.homedatazip.tradeRent.util;

public final class JibunKey {
    private JibunKey() {}

    // 전월세 row(LOTNO_SE, MNO, SNO) -> "산 272-2" / "272-2" / "272"
    public static String fromRentRow(RentRow row) {
        String mnoRaw = trim(row.MNO());
        if (mnoRaw.isEmpty()) return ""; // 본번 없으면 지번키 만들 수 없음

        Integer mno = parseIntLoose(mnoRaw);
        if (mno == null) return "";

        String snoRaw = trim(row.SNO());
        Integer sno = parseIntLoose(snoRaw); // 부번이 비어있으면 null

        boolean isMountain = isMountain(row.LOTNO_SE(), row.LOTNO_SE_NM());

        String base = (sno != null && sno != 0) ? (mno + "-" + sno) : String.valueOf(mno);
        return (isMountain ? "산 " : "") + base;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    // "0001" -> 1, "" -> null, "  " -> null
    private static Integer parseIntLoose(String s) {
        String t = trim(s);
        if (t.isEmpty()) return null;
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // LOTNO_SE/LOTNO_SE_NM 값 체계가 확실치 않으면 NM까지 같이 본다
    private static boolean isMountain(String lotnoSe, String lotnoSeNm) {
        String se = trim(lotnoSe);
        String nm = trim(lotnoSeNm);
        // 보통 "산" 또는 코드값으로 구분됨. NM이 "산"이면 확정.
        if (nm.contains("산")) return true;
        // 코드가 2가 산인 케이스가 흔해서 보수적으로 포함
        return "2".equals(se);
    }
}
