package org.example.homedatazip.tradeRent.dto;

public record ApartmentGetOrCreateRequest(
        String aptSeq,          // 있으면 최우선 키
        String sggCd,           // 법정동 5자리 (지오코더 검색 범위/필터)
        String umdNm,           // 법정동명 (지번 주소 구성에 도움)
        String jibun,           // "232" 같은 원문
        String jibunAddress,    // "창신동 232" (없으면 null 가능)
        String roadNm,     // 전월세 DTO에 없으면 null (추후 확장 가능)
        String roadBonBun,
        String roadBuBun,
        String aptName,         // 전월세 DTO에 없으면 null
        Integer buildYear       // 전월세 DTO에 없으면 null
) {
    public static ApartmentGetOrCreateRequest from(TradeRentWriteRequest tr) {
        if (tr == null) return null;
        return new ApartmentGetOrCreateRequest(
                blankToNull(tr.aptSeq()),
                blankToNull(tr.sggCd()),
                blankToNull(tr.umdNm()),
                blankToNull(tr.jibun()),
                blankToNull(tr.jibunKey()), // 이미 "umdNm + jibun" 형태라면 그대로 사용
                blankToNull(tr.roadnm()),
                blankToNull(tr.roadnmbonbun()),
                blankToNull(tr.roadnmbubun()),
                blankToNull(tr.aptName()),                    // aptName
                tr.buildYear()                        // buildYear
        );
    }




    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}