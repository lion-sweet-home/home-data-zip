package org.example.homedatazip.tradeRent.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RentApiItem {

    @JacksonXmlProperty(localName = "aptNm")
    private String aptNm;

    @JacksonXmlProperty(localName = "aptSeq")
    private String aptSeq;

    @JacksonXmlProperty(localName = "buildYear")
    private String buildYear;

    @JacksonXmlProperty(localName = "contractTerm")
    private String contractTerm;

    @JacksonXmlProperty(localName = "contractType")
    private String contractType;

    @JacksonXmlProperty(localName = "dealYear")
    private String dealYear;

    @JacksonXmlProperty(localName = "dealMonth")
    private String dealMonth;

    @JacksonXmlProperty(localName = "dealDay")
    private String dealDay;

    @JacksonXmlProperty(localName = "deposit")
    private String deposit;

    @JacksonXmlProperty(localName = "monthlyRent")
    private String monthlyRent;

    @JacksonXmlProperty(localName = "preDeposit")
    private String preDeposit;

    @JacksonXmlProperty(localName = "preMonthlyRent")
    private String preMonthlyRent;

    @JacksonXmlProperty(localName = "excluUseAr")
    private String excluUseAr;

    @JacksonXmlProperty(localName = "floor")
    private String floor;

    @JacksonXmlProperty(localName = "jibun")
    private String jibun;

    @JacksonXmlProperty(localName = "roadnm")
    private String roadnm;

    @JacksonXmlProperty(localName = "roadnmbcd")
    private String roadnmbcd;

    @JacksonXmlProperty(localName = "roadnmbonbun")
    private String roadnmbonbun;

    @JacksonXmlProperty(localName = "roadnmbubun")
    private String roadnmbubun;

    @JacksonXmlProperty(localName = "roadnmcd")
    private String roadnmcd;

    @JacksonXmlProperty(localName = "roadnmseq")
    private String roadnmseq;

    @JacksonXmlProperty(localName = "roadnmsggcd")
    private String roadnmsggcd;

    @JacksonXmlProperty(localName = "sggCd")
    private String sggCd;

    @JacksonXmlProperty(localName = "umdNm")
    private String umdNm;

    @JacksonXmlProperty(localName = "useRRRight")
    private String useRRRight;
}
