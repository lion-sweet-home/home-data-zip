package org.example.homedatazip.global.batch.hospital.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.exception.BatchSkipException;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.example.homedatazip.hospital.entity.Hospital;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class HospitalRegionProcessor implements ItemProcessor<Hospital, Hospital> {

    private final GeoService geoService;

    // 좌표를 반올림하여 Region Cache에 저장
    private final Map<String, Region> regionCache = new ConcurrentHashMap<>();

    @Override
    public Hospital process(Hospital hospital) throws Exception {
        if (hospital.getLatitude() == null || hospital.getLongitude() == null) {
            log.warn("⏭️ 위경도 누락 (스킵) - Hospital Id: {}, Hospital: {}, 위도: {}, 경도: {}",
                    hospital.getHospitalId(),
                    hospital.getName(),
                    hospital.getLatitude(),
                    hospital.getLongitude()
            );
            return null;
        }

        // 좌표를 소수점 3자리로 반올림 (약 111m 정밀도)
        String cacheKey = roundCoordinate(hospital.getLatitude(), hospital.getLongitude());

        Region region = regionCache.computeIfAbsent(cacheKey, key -> {
                    try {
                        return geoService.convertAddressInfoInNewTransaction(
                                hospital.getLatitude(),
                                hospital.getLongitude()
                        );
                    } catch (BatchSkipException e) {
                        log.warn("⏭️ Region 매칭 실패 (스킵) - Hospital Id: {}, Hospital: {}, 사유: {}",
                                hospital.getHospitalId(),
                                hospital.getName(),
                                e.getMessage()
                        );
                        return null;
                    }
                }
        );

        if (region == null) {
            log.warn("⏭️ Region 누락 - Hospital Id: {}, Hospital: {}",
                    hospital.getHospitalId(),
                    hospital.getName()
            );
            return null;
        }

        hospital.attachRegion(region);
        return hospital;
    }

    private String roundCoordinate(Double latitude, Double longitude) {
        // 소수점 3자리 => 약 111m 반경
        return String.format("%.3f, %.3f",
                Math.round(latitude * 1000.0) / 1000.0,
                Math.round(longitude * 1000.0) / 1000.0
        );
    }
}
