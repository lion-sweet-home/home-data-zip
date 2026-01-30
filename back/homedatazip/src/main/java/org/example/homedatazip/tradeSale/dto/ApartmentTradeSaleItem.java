package org.example.homedatazip.tradeSale.dto;

public record ApartmentTradeSaleItem(
        String sggCd, // 지역코드
        String umdCd, // 법정동코드
        String aptNm, // 아파트명
        String jibun, // 지번
        String excluUseAr, // 전용면적
        String dealYear, // 계약년
        String dealMonth, // 계약월
        String dealDay, // 계약일
        String dealAmount, // 거래금액
        String floor, // 층
        String buildYear, // 건축년도
        String aptSeq, // 아파트 일련번호 (Unique Key)
        String roadNm, // 도로명
        String umdNm, // 법정동명
        String aptDong, // 아파트 동
        String cdealType, // 해제여부
        String cdealDay, // 해제사유발생일
        String roadNmBonbun,
        String roadNmBubun
) {}
