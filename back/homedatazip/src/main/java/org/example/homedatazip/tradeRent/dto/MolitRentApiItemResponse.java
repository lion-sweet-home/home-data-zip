package org.example.homedatazip.tradeRent.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record MolitRentApiItemResponse (
        @JacksonXmlProperty(localName = "sggCd")
        String sggCd,

        @JacksonXmlProperty(localName = "umdNm")
        String umdNm,

        @JacksonXmlProperty(localName = "aptNm")
        String aptNm,

        @JacksonXmlProperty(localName = "jibun")
        String jibun,

        @JacksonXmlProperty(localName = "excluUseAr")
        String excluUseAr,

        @JacksonXmlProperty(localName = "dealYear")
        String dealYear,

        @JacksonXmlProperty(localName = "dealMonth")
        String dealMonth,

        @JacksonXmlProperty(localName = "dealDay")
        String dealDay,

        @JacksonXmlProperty(localName = "deposit")
        String deposit,

        @JacksonXmlProperty(localName = "monthlyRent")
        String monthlyRent,

        @JacksonXmlProperty(localName = "floor")
        String floor,

        @JacksonXmlProperty(localName = "buildYear")
        String buildYear,

        @JacksonXmlProperty(localName = "contractTerm")
        String contractTerm,

        @JacksonXmlProperty(localName = "contractType")
        String contractType,

        @JacksonXmlProperty(localName = "useRRRight")
        String useRRRight,

        @JacksonXmlProperty(localName = "preDeposit")
        String preDeposit,

        @JacksonXmlProperty(localName = "preMonthlyRent")
        String preMonthlyRent
){}
