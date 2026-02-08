package org.example.homedatazip.global.batch.hospital.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.exception.BatchSkipException;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.example.homedatazip.hospital.entity.Hospital;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HospitalRegionProcessor implements ItemProcessor<Hospital, Hospital> {

    private final GeoService geoService;

    @Override
    public Hospital process(Hospital hospital) throws Exception {

        try {
            Region region = geoService.convertAddressInfoInNewTransaction(
                    hospital.getLatitude(),
                    hospital.getLongitude()
            );

            hospital.attachRegion(region);
            return hospital;
        } catch (BatchSkipException e) {
            log.warn("⏭️ Region 매칭 실패 (스킵) - Hospital: {}, 사유: {}",
                    hospital.getName(),
                    e.getMessage()
            );
            return null;
        }
    }
}
