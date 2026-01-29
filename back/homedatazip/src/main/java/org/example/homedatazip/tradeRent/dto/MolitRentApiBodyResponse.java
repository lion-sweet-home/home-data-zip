package org.example.homedatazip.tradeRent.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record MolitRentApiBodyResponse (
    @JacksonXmlProperty(localName = "items")
    MolitRentApiItemsResponse items,

    @JacksonXmlProperty(localName = "totalCount")
    Integer totalcount,

    @JacksonXmlProperty(localName = "numOfRows")
    Integer numOfRows,

    @JacksonXmlProperty(localName = "pageNo")
    Integer pageNo
){}
