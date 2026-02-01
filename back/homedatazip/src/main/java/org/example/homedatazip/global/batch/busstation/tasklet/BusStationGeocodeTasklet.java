package org.example.homedatazip.global.batch.busstation.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.repository.BusStationRepository;
import org.example.homedatazip.global.geocode.service.GeoService;
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
    private final GeoService geoService;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        int totalSuccess = 0, totalFail = 0, totalSkipped = 0;

        while (true) {
            var targets = busStationRepository.findTop500ByRegionIsNullOrderByIdAsc();
            if (targets.isEmpty()) {
                log.info("[BUS-GEO] done. success={}, fail={}, skipped={}", totalSuccess, totalFail, totalSkipped);
                return RepeatStatus.FINISHED;
            }

            int success = 0, fail = 0, skipped = 0;

            for (var station : targets) {
                Double lat = station.getLatitude();
                Double lon = station.getLongitude();

                if (lat == null || lon == null) {
                    skipped++;
                    continue;
                }

                try {
                    var region = geoService.convertAddressInfo(lat, lon);
                    if (region == null) {
                        skipped++;
                        continue;
                    }
                    station.attachRegion(region);
                    success++;
                } catch (Exception e) {
                    log.warn("[BUS-GEO] fail stationId={}, nodeId={}, lat={}, lon={}, err={}",
                            station.getId(), station.getNodeId(), lat, lon, e.getMessage());
                    fail++;
                }
            }

            totalSuccess += success;
            totalFail += fail;
            totalSkipped += skipped;

            log.info("[BUS-GEO] batch success={}, fail={}, skipped={}, processed={}",
                    success, fail, skipped, targets.size());

            // 무한루프 방지: 이번 배치에서 성공이 0이면 더 돌려봐야 의미가 없음
            if (success == 0) {
                log.warn("[BUS-GEO] stop: no progress in this batch. (success=0) totalFail={}, totalSkipped={}",
                        totalFail, totalSkipped);
                return RepeatStatus.FINISHED;
            }
        }
    }
}
