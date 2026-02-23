package org.example.homedatazip.global.batch.busstation.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.busstation.repository.BusStationRepository;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusStationGeocodeTasklet implements Tasklet {

    private final BusStationRepository busStationRepository;
    private final GeoService geoService;

    // ===== 튜닝 포인트 =====
    private static final int BATCH_SIZE = 500;
    private static final int MAX_RETRY = 3;
    private static final long BACKOFF_MS = 500L;      // 기본 대기
    private static final long BACKOFF_429_MS = 1500L; // 429(Too Many Requests)면 더 대기
    private static final int NO_PROGRESS_STOP_THRESHOLD = 2; // success=0 연속 N번이면 종료

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        int totalSuccess = 0, totalFail = 0, totalSkipped = 0;
        int noProgressCount = 0;

        while (true) {
            var targets = busStationRepository.findTop500ByRegionIsNullOrderByIdAsc(); // BATCH_SIZE 쓰려면 repo 메서드도 맞춰야 함
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
                    Region region = callWithRetry(lat, lon, station.getId(), station.getNodeId());
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


            if (success == 0) {
                noProgressCount++;
                log.warn("[BUS-GEO] no progress. count={}/{} (totalFail={}, totalSkipped={})",
                        noProgressCount, NO_PROGRESS_STOP_THRESHOLD, totalFail, totalSkipped);

                if (noProgressCount >= NO_PROGRESS_STOP_THRESHOLD) {
                    log.warn("[BUS-GEO] stop: no progress repeatedly.");
                    return RepeatStatus.FINISHED;
                }
            } else {
                noProgressCount = 0;
            }
        }
    }

    /**
     * GeoService 호출을 retry/backoff 포함해서 감싼다.
     */
    private Region callWithRetry(double lat, double lon, Long stationId, String nodeId) {
        Exception last = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                return geoService.convertAddressInfo(lat, lon);

            } catch (WebClientResponseException e) {
                last = e;

                int code = e.getStatusCode().value();

                // 4xx 중에서도 재시도 가치 없는 애들
                // - 401/403: 키/권한 문제라 재시도 의미 없음
                // - 400: 파라미터 문제
                if (code == 401 || code == 403 || code == 400) {
                    log.warn("[BUS-GEO] no-retry ({}). stationId={}, nodeId={}, msg={}",
                            code, stationId, nodeId, e.getMessage());
                    throw e;
                }

                // 429 / 5xx 는 재시도 가치 있음
                if (attempt < MAX_RETRY) {
                    long wait = (code == 429) ? BACKOFF_429_MS : BACKOFF_MS;
                    log.warn("[BUS-GEO] retry {}/{} (status={}) stationId={}, nodeId={}, wait={}ms",
                            attempt, MAX_RETRY, code, stationId, nodeId, wait);
                    sleep(wait);
                    continue;
                }

                // 마지막까지 실패
                throw e;

            } catch (Exception e) {
                last = e;

                if (attempt < MAX_RETRY) {
                    log.warn("[BUS-GEO] retry {}/{} stationId={}, nodeId={}, wait={}ms, err={}",
                            attempt, MAX_RETRY, stationId, nodeId, BACKOFF_MS, e.getMessage());
                    sleep(BACKOFF_MS);
                    continue;
                }
                throw e;
            }
        }

        // 이론상 도달 안 함
        if (last != null) throw new RuntimeException(last);
        return null;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
