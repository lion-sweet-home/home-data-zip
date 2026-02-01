package org.example.homedatazip.global.batch.busstation.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.client.dto.SeoulBusStopResponse;
import org.example.homedatazip.busstation.entity.BusStation;
import org.example.homedatazip.busstation.mapper.BusStationMapper;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusStationProcessor implements ItemProcessor<SeoulBusStopResponse.Row, BusStation> {

    private final BusStationMapper mapper;
    private final GeoService geoService;

    @Override
    public BusStation process(SeoulBusStopResponse.Row row) {
        if (row == null) return null;

        String nodeId = mapper.nodeId(row);
        if (isBlank(nodeId)) return null;

        Double longitude = mapper.longitude(row); // XCRD
        Double latitude = mapper.latitude(row);   // YCRD

        if (!isValidCoord(latitude, longitude)) return null;

        Region region;
        try {
            region = geoService.convertAddressInfo(latitude, longitude);
        } catch (Exception e) {
            log.warn("[BUS_STATION] region resolve fail. nodeId={} lat={} lon={}", nodeId, latitude, longitude);
            return null;
        }

        String stationNumber = mapper.stationNumber(row);
        String name = normalizeName(mapper.name(row));

        BusStation station = new BusStation(nodeId);
        station.update(
                stationNumber,
                name,
                longitude,
                latitude,
                region
        );

        return station;
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private boolean isValidCoord(Double lat, Double lon) {
        if (lat == null || lon == null) return false;
        if (lat < -90 || lat > 90) return false;
        if (lon < -180 || lon > 180) return false;
        return true;
    }

    private String normalizeName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.replaceAll("\\s+", " ");
    }
}
