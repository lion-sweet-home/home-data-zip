package org.example.homedatazip.tradeSale.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor // Jackson이 객체를 생성할 수 있게 해줌
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 없는 필드는 무시해서 에러 방지
public class ApartmentTradeSaleItem {

    @JacksonXmlProperty(localName = "sggCd") private String sggCd;
    @JacksonXmlProperty(localName = "umdCd") private String umdCd;
    @JacksonXmlProperty(localName = "aptNm") private String aptNm;
    @JacksonXmlProperty(localName = "jibun") private String jibun;
    @JacksonXmlProperty(localName = "excluUseAr") private String excluUseAr;
    @JacksonXmlProperty(localName = "dealYear") private String dealYear;
    @JacksonXmlProperty(localName = "dealMonth") private String dealMonth;
    @JacksonXmlProperty(localName = "dealDay") private String dealDay;
    @JacksonXmlProperty(localName = "dealAmount") private String dealAmount;
    @JacksonXmlProperty(localName = "floor") private String floor;
    @JacksonXmlProperty(localName = "buildYear") private String buildYear;
    @JacksonXmlProperty(localName = "aptSeq") private String aptSeq;
    @JacksonXmlProperty(localName = "roadNm") private String roadNm;
    @JacksonXmlProperty(localName = "umdNm") private String umdNm;
    @JacksonXmlProperty(localName = "aptDong") private String aptDong;
    @JacksonXmlProperty(localName = "cdealType") private String cdealType;
    @JacksonXmlProperty(localName = "cdealDay") private String cdealDay;
    @JacksonXmlProperty(localName = "roadNmBonbun") private String roadNmBonbun;
    @JacksonXmlProperty(localName = "roadNmBubun") private String roadNmBubun;
}