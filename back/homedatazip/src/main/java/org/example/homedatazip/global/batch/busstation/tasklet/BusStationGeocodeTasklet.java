package org.example.homedatazip.global.batch.busstation.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.repository.BusStationRepository;
import org.example.homedatazip.data.repository.RegionRepository;
import org.example.homedatazip.global.geocode.service.KakaoApiClient;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusStationGeocodeTasklet implements Tasklet {

    private final BusStationRepository busStationRepository;
    private final KakaoApiClient kakaoApiClient;
    private final RegionRepository regionRepository;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        var targets = busStationRepository.findTop500ByRegionIsNullOrderByIdAsc();
        if (targets.isEmpty()) {
            log.info("[BUS-GEO] nothing to enrich");
            return RepeatStatus.FINISHED;
        }

        int success = 0;
        int fail = 0;

        for (var station : targets) {
            if (station.getLatitude() == null || station.getLongitude() == null) {
                fail++;
                continue;
            }

            try {
                var res = kakaoApiClient.getAddressByCoordinate(station.getLatitude(), station.getLongitude());
                if (res == null || res.documents() == null || res.documents().isEmpty()) {
                    fail++;
                    continue;
                }

                var doc = res.documents().stream()
                        .filter(d -> "B".equalsIgnoreCase(d.regionType()))
                        .findFirst()
                        .orElse(res.documents().get(0));

                String lawd10 = toLawd10(doc.code());
                if (lawd10 == null) {
                    fail++;
                    continue;
                }

                var region = regionRepository.findByLawdCode(lawd10).orElse(null);
                if (region == null) {
                    fail++;
                    continue;
                }

                station.attachRegion(region);
                success++;

            } catch (Exception e) {
                fail++;
            }
        }

        log.info("[BUS-GEO] success={}, fail={}, processed={}", success, fail, targets.size());

        // 반복 실행되게: 아직 region null이 남아있으면 CONTINUABLE로 돌려도 되는데
        // 여기선 Step을 스케줄로 여러번 돌리면 됨.
        return RepeatStatus.FINISHED;
    }

    private String toLawd10(String kakaoCode) {
        if (kakaoCode == null) return null;
        String digits = kakaoCode.replaceAll("\\D", "");
        if (digits.length() < 10) return null;
        return digits.substring(0, 10);
    }
}
