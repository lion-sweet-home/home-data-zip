package org.example.homedatazip.global.batch.hospital.processor;

import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.hospital.dto.HospitalApiResponse;
import org.example.homedatazip.hospital.entity.Hospital;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Processor: API 응답 -> Entity 변환
 * <br/>
 * Reader에서 읽은 데이터를 가공 혹은 필터링
 */
@Slf4j
@Component
public class HospitalImportProcessor implements ItemProcessor<HospitalApiResponse.HospitalItem, Hospital> {

    @Override
    public Hospital process(HospitalApiResponse.HospitalItem row) throws Exception {
        // 필수 데이터 검증
        if (row.getHospitalId() == null || row.getName() == null) {
            log.warn("⚠️ 데이터 누락- ID: {}, 이름: {}",
                    row.getHospitalId(),
                    row.getName()
            );
            return null;
        }

        // 지역 필터링 (서울, 인천만 저장)
        if (!isTargetRegion(row.getAddress())) {
            log.debug("⏭️ 저장하지 않는 지역 - {}", row.getAddress());
            return null; // 해당 데이터는 저장하지 않음
        }

        return Hospital.fromApiResponse(
                row.getHospitalId(),
                row.getName(),
                row.getTypeName(),
                null,
                row.getAddress(),
                row.getLatitude(),
                row.getLongitude()
        );
    }

    /**
     * 서울, 인천 필터링
     */
    private boolean isTargetRegion(String address) {
        if (address == null || address.isEmpty()) return false;

        return address.startsWith("서울")
                || address.startsWith("인천");
    }
}
