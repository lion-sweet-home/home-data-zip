package org.example.homedatazip.hospital.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.hospital.dto.HospitalStatsResponse;
import org.example.homedatazip.hospital.repository.HospitalRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public Long getHospitalCountByDong(String dong) {
        return hospitalRepository.countByDong(dong);
    }

    public HospitalStatsResponse getHospitalStatsByDong(String dong) {
        List<Object[]> results = hospitalRepository.countByTypeNameAndDong(dong);

        Map<String, Long> countByTypeName = results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        long totalCount = countByTypeName.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        return new HospitalStatsResponse(dong, totalCount, countByTypeName);
    }
}
