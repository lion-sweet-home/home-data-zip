package org.example.homedatazip.global.geocode.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.geocode.dto.CoordinateInfoResponse;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class TestGeoController {

    private final GeoService geoService;

    @PostMapping("/geo-test")
    public ResponseEntity<CoordinateInfoResponse> geoTest(@RequestParam String dong, @RequestParam String jibun) {

        log.info("GeoCoding Test Start - dong={}, jibun={}", dong, jibun);

        CoordinateInfoResponse coordinateInfoResponse = geoService.convertCoordinateInfo(dong, jibun);

        log.info("GeoCoding Test Success - latitude={}, longitude={}",
                coordinateInfoResponse.latitude(),
                coordinateInfoResponse.longitude());

        return ResponseEntity.ok(coordinateInfoResponse);
    }

    @PostMapping("reverse-geo-test")
    public ResponseEntity<Region> reverseGeoTest(@RequestParam String latitudeStr,
                                                              @RequestParam String longitudeStr) {

        log.info("Reverse GeoCoding Test Start - latitude={}, longitude={}", latitudeStr, longitudeStr);

        Double latitude = Double.parseDouble(latitudeStr);
        Double longitude = Double.parseDouble(longitudeStr);

        Region region = geoService.convertAddressInfo(latitude, longitude);

        log.info("Reverse GeoCoding Test Success - region={}", region);

        return ResponseEntity.ok(region);
    }
}
