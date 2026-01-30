package org.example.homedatazip.apartment.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;
@JacksonXmlRootElement(localName="response")
public record ApiResponse<T>(
        ResponseHeader header,
        ResponseBody<T> body
) {
    public record ResponseHeader(
            String resultCode,
            String resultMsg
    ) {}

    public record ResponseBody<T>(
            ResponseItems<T> items,
            int numOfRows,
            int pageNo,
            int totalCount
    ) {}

    public record ResponseItems<T>(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "item")
            List<T> item
    ) {
        public List<T> safeItem() {
            return item == null ? List.of() : item;
        }
    }
}
