package org.example.homedatazip.subway.batch.processor;

import org.example.homedatazip.subway.batch.dto.SubwayStationSourceSync;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class StationApiToSourceSyncProcessor implements ItemProcessor<SubwayStationSourceSync, SubwayStationSourceSync> {
    // OpenAPI → Writer, 검증만 수행: 필수 필드 null/공백이면 스킵
    @Override
    public SubwayStationSourceSync process(SubwayStationSourceSync item) {
        if (item == null) return null;

        if (trimToNull(item.lineStationCode()) == null
                || trimToNull(item.stationName()) == null
                || trimToNull(item.lineName()) == null
                || item.latitude() == null
                || item.longitude() == null) {
            return null;
        }
        return item;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}
