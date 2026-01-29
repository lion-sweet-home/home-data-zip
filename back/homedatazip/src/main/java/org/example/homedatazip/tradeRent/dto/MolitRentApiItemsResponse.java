package org.example.homedatazip.tradeRent.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public record MolitRentApiItemsResponse(
        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<MolitRentApiItemResponse> items
){
    public List<MolitRentApiItemResponse> safeItem(){
        return items == null ? List.of() : items;
    }
}
