package org.example.homedatazip.global.batch.busstation.processor;

import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class BusStationProcessor implements ItemProcessor<SeoulBusStopResponse.Row, SeoulBusStopResponse.Row> {

    @Override
    public SeoulBusStopResponse.Row process(SeoulBusStopResponse.Row item) {
        if (item == null) return null;

        if (item.NODE_ID() == null || item.NODE_ID().isBlank()) return null;
        if (item.STOPS_NM() == null || item.STOPS_NM().isBlank()) return null;

        return item;
    }
}
