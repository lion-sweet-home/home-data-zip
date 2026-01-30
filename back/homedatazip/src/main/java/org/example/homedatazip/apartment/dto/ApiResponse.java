package org.example.homedatazip.apartment.dto;

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
            List<T> item
    ) {}
}
