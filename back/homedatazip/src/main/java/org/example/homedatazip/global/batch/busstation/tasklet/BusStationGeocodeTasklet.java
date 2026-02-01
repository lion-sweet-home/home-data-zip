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

        var targets = busStationRepository.findTop500ByRegionIsNullOrderByIdAsc();
        if (targets.isEmpty()) {
            log.info("[BUS-GEO] nothing to enrich");
            return RepeatStatus.FINISHED;
        }

        int success = 0;
        int fail = 0;
        int skipped = 0;

        for (var station : targets) {
            Double lat = station.getLatitude();
            Double lon = station.getLongitude();

            if (lat == null || lon == null) {
                skipped++;
                continue;
            }

            try {
                // ✅ 여기서 끝: 좌표 -> 카카오 -> bCode -> Region 조회까지 완료
                var region = geoService.convertAddressInfo(lat, lon);

                // attachRegion 메서드가 있으면 그대로 쓰고,
                // 없으면 station.update(..., region) 형태로 바꿔.
                station.attachRegion(region);

                success++;
            } catch (Exception e) {
                // 실패 로그는 남겨야 나중에 이유 찾는다
                log.warn("[BUS-GEO] fail stationId={}, nodeId={}, lat={}, lon={}, err={}",
                        station.getId(), station.getNodeId(), lat, lon, e.getMessage());
                fail++;
            }
        }

        log.info("[BUS-GEO] success={}, fail={}, skipped={}, processed={}",
                success, fail, skipped, targets.size());

        return RepeatStatus.FINISHED;
    }
}
