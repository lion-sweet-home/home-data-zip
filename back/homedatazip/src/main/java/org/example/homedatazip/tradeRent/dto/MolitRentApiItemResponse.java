package org.example.homedatazip.tradeRent.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record MolitRentApiItemResponse (
        @JacksonXmlProperty(localName = "aptNm") String aptNm,
        @JacksonXmlProperty(localName = "aptSeq") String aptSeq,
        @JacksonXmlProperty(localName = "buildYear") String buildYear,

        @JacksonXmlProperty(localName = "contractTerm") String contractTerm,
        @JacksonXmlProperty(localName = "contractType") String contractType,

        @JacksonXmlProperty(localName = "dealYear") String dealYear,
        @JacksonXmlProperty(localName = "dealMonth") String dealMonth,
        @JacksonXmlProperty(localName = "dealDay") String dealDay,

        @JacksonXmlProperty(localName = "deposit") String deposit,
        @JacksonXmlProperty(localName = "monthlyRent") String monthlyRent,
        @JacksonXmlProperty(localName = "preDeposit") String preDeposit,
        @JacksonXmlProperty(localName = "preMonthlyRent") String preMonthlyRent,

        @JacksonXmlProperty(localName = "excluUseAr") String excluUseAr,
        @JacksonXmlProperty(localName = "floor") String floor,

        @JacksonXmlProperty(localName = "jibun") String jibun,
        @JacksonXmlProperty(localName = "roadnm") String roadnm,
        @JacksonXmlProperty(localName = "roadnmbcd") String roadnmbcd,
        @JacksonXmlProperty(localName = "roadnmbonbun") String roadnmbonbun,
        @JacksonXmlProperty(localName = "roadnmbubun") String roadnmbubun,
        @JacksonXmlProperty(localName = "roadnmcd") String roadnmcd,
        @JacksonXmlProperty(localName = "roadnmseq") String roadnmseq,
        @JacksonXmlProperty(localName = "roadnmsggcd") String roadnmsggcd,

        @JacksonXmlProperty(localName = "sggCd") String sggCd,
        @JacksonXmlProperty(localName = "umdNm") String umdNm,
        @JacksonXmlProperty(localName = "useRRRight") String useRRRight
){}
