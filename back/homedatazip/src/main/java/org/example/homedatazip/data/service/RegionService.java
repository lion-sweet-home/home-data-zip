package org.example.homedatazip.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.data.dto.RegionApiResponse;
import org.example.homedatazip.data.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RegionService {

    private final RegionRepository regionRepository;

    // API DTO 를 엔티티로 저장
    @Transactional
    public void upsertRegions(List<RegionApiResponse> responses) {

        List<String> lawdCodes = responses.stream()
                .map(RegionApiResponse::lawdCode)
                .distinct()
                .toList();

        Map<String, Region> existingMap = regionRepository.findByLawdCodeIn(lawdCodes).stream()
                .collect(Collectors.toMap(
                        Region::getLawdCode,
                        r -> r,
                        (oldValue, newValue) -> oldValue
                ));

        List<Region> toSave = new ArrayList<>();

        for (RegionApiResponse response : responses) {
            Region region = existingMap.get(response.lawdCode());
            if (region != null) {
                region.updateFrom(response);
            } else {
                region = Region.from(response);
            }

            toSave.add(region);
        }

        // Repo 저장
        if (!toSave.isEmpty()) {
            regionRepository.saveAll(toSave);
            log.info("Region 데이터 {}건을 저장/업데이트 했습니다.", toSave.size());
        }

    }

    // 중복 없는 5자리 시군구 코드 리스트(Open API 호출 루프)
    public List<String> getDistinctSggCodes() {
        return regionRepository.findDistinctSggCode();
    }

    // 시/도 목록 보기
    public List<String> findSidoList() {
        return regionRepository.findDistinctSido();
    }

    // 선택한 시/도 에서 구/군 목록 보기
    public List<String> findGugunList(String sido) {
        return regionRepository.findDistinctGugunBySido(sido);
    }

    // 선택한 구/군 에서 동 목록 보기
    public List<String> findDongList(String sido, String gugun) {
        return regionRepository.findBySidoAndGugun(sido, gugun);
    }
}
