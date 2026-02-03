package org.example.homedatazip.hospital.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.data.repository.RegionRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.RegionErrorCode;
import org.example.homedatazip.hospital.dto.HospitalResponse;
import org.example.homedatazip.hospital.dto.HospitalStatsResponse;
import org.example.homedatazip.hospital.entity.Hospital;
import org.example.homedatazip.hospital.repository.HospitalRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final RegionRepository regionRepository;

    /**
     * 병원 개수 조회
     */
    public Long getHospitalCount(
            String sido,
            String gugun,
            String dong
    ) {
        Region region = findRegion(sido, gugun, dong);

        return (long) hospitalRepository.findByRegion(region).size();
    }

    /**
     * 병원 종류별 개수 조회
     */
    public HospitalStatsResponse getHospitalStats(
            String sido,
            String gugun,
            String dong
    ) {
        Region region = findRegion(sido, gugun, dong);

        List<Hospital> hospitals = hospitalRepository.findByRegion(region);

        Map<String, Long> countByTypeName = hospitals.stream()
                .collect(Collectors.groupingBy(
                        Hospital::getTypeName,
                        Collectors.counting()
                ));

        return new HospitalStatsResponse(
                sido,
                gugun,
                dong,
                (long) hospitals.size(),
                countByTypeName
        );
    }

    /**
     * 병원 목록 조회 (마커용)
     */
    public List<HospitalResponse> getHospitalList(
            String sido,
            String gugun,
            String dong
    ) {
        Region region = findRegion(sido, gugun, dong);

        List<Hospital> hospitals = hospitalRepository.findByRegion(region);

        return hospitals.stream()
                .map(HospitalResponse::from)
                .toList();
    }

    private Region findRegion(String sido, String gugun, String dong) {
        return regionRepository
                .findBySidoAndGugunAndDong(sido, gugun, dong)
                .orElseThrow(() ->
                        new BusinessException(RegionErrorCode.REGION_NOT_FOUND));
    }
}
