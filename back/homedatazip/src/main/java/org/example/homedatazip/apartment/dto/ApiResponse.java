package org.example.homedatazip.apartment.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

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
            @JacksonXmlElementWrapper(useWrapping = false) // <item> 태그가 반복될 때 래퍼가 없음을 명시
            @JacksonXmlProperty(localName = "item")
            List<T> item
    ) {}
}
