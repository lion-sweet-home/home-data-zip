package org.example.homedatazip.busstation.batch;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusStationProcessor implements ItemProcessor<SeoulBusStopResponse.Row, BusStation> {

    private final GeoService geoService;

    @Override
    public BusStation process(SeoulBusStopResponse.Row row) {
        Double lon = Double.valueOf(row.XCRD());
        Double lat = Double.valueOf(row.YCRD());

        Region region = geoService.convertAddressInfo(lat, lon);

        BusStation station = new BusStation(row.NODE_ID());
        station.update(
                row.STOPS_NO(),
                row.STOPS_NM(),
                lon,
                lat,
                region
        );
        return station;
    }
}
