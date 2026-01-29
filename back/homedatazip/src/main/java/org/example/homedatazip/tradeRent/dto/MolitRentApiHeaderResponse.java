package org.example.homedatazip.tradeRent.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record MolitRentApiHeaderResponse (
        @JacksonXmlProperty(localName = "resultCode")
        String resultCode,
        @JacksonXmlProperty(localName = "resulMsg")
        String resulMsg

){}
