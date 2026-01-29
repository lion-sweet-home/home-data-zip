package org.example.homedatazip.tradeRent.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "response")
public record MolitRentApiResponse (
        @JacksonXmlProperty(localName = "header")
        MolitRentApiHeaderResponse header,

        @JacksonXmlProperty(localName = "body")
        MolitRentApiBodyResponse body

) {
}