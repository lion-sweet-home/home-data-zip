package org.example.homedatazip.busstation.mapper;

import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.springframework.stereotype.Component;

@Component
public class BusStationMapper {

    public String nodeId(SeoulBusStopResponse.Row row) {
        return row.NODE_ID();
    }

    public String stationNumber(SeoulBusStopResponse.Row row) {
        return row.STOPS_NO();
    }

    public String name(SeoulBusStopResponse.Row row) {
        return row.STOPS_NM();
    }

    public Double longitude(SeoulBusStopResponse.Row row) {
        return parseDouble(row.XCRD());
    }

    public Double latitude(SeoulBusStopResponse.Row row) {
        return parseDouble(row.YCRD());
    }

    private Double parseDouble(String v) {
        if (v == null || v.isBlank()) return null;
        return Double.valueOf(v);
    }
}
