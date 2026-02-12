package org.example.homedatazip.global.batch.apartment.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.entity.ApartmentSubwayDistance;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.apartment.repository.ApartmentSubwayDistanceRepository;
import org.example.homedatazip.global.util.HaversineUtils;
import org.example.homedatazip.subway.entity.SubwayStation;
import org.example.homedatazip.subway.repository.SubwayStationRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 아파트–지하철역 거리(하버사인) 계산 후 10km 이내만 apartment_subway_distances 에 적재.
 * 기존 데이터는 전부 삭제 후 재적재(Full Refresh).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApartmentSubwayDistanceTasklet implements Tasklet {

    private static final double MAX_RADIUS_KM = 10.0;
    private static final int SAVE_CHUNK_SIZE = 500;
    private static final int PROGRESS_LOG_EVERY_APT = 200;

    private final ApartmentRepository apartmentRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final ApartmentSubwayDistanceRepository apartmentSubwayDistanceRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<Apartment> apartments = apartmentRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull();
        List<SubwayStation> stations = subwayStationRepository.findAll().stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .toList();

        if (apartments.isEmpty() || stations.isEmpty()) {
            log.warn("아파트 또는 지하철역이 없어 거리 계산을 건너뜁니다. apartments={}, stations={}",
                    apartments.size(), stations.size());
            return RepeatStatus.FINISHED;
        }

        log.info("아파트–지하철 거리 배치 시작: apartments={}, stations={}, maxRadiusKm={}, saveChunkSize={}",
                apartments.size(), stations.size(), MAX_RADIUS_KM, SAVE_CHUNK_SIZE);

        // 기존 데이터 삭제 (Full Refresh)
        log.info("apartment_subway_distances 기존 데이터 삭제 시작");
        apartmentSubwayDistanceRepository.deleteAllInBatch();
        log.info("apartment_subway_distances 기존 데이터 삭제 완료");

        List<ApartmentSubwayDistance> toInsert = new ArrayList<>();
        for (int ai = 0; ai < apartments.size(); ai++) {
            Apartment apt = apartments.get(ai);
            Long aptId = apt.getId();
            String aptName = apt.getAptName();
            Double aptLat = apt.getLatitude();
            Double aptLon = apt.getLongitude();

            if (ai % PROGRESS_LOG_EVERY_APT == 0) {
                log.info("진행률: {}/{} 아파트 처리 중... (현재 aptId={}, aptName={}, 누적Insert={})",
                        ai, apartments.size(), aptId, aptName, toInsert.size());
            }

            try {
            for (SubwayStation station : stations) {
                double distanceKm = HaversineUtils.distanceKm(
                            aptLat, aptLon,
                        station.getLatitude(), station.getLongitude());
                if (distanceKm <= MAX_RADIUS_KM) {
                    toInsert.add(ApartmentSubwayDistance.of(apt, station, distanceKm));
                }
            }
            } catch (Exception e) {
                // 어떤 아파트에서 터졌는지 확실히 남기기
                log.error("아파트–지하철 거리 계산 중 예외 발생. aptId={}, aptName={}, lat={}, lon={}, stationsSize={}",
                        aptId, aptName, aptLat, aptLon, stations.size(), e);
                throw e; // 배치 실패로 처리
            }
        }

        log.info("저장 시작: totalToInsert={}", toInsert.size());
        for (int i = 0; i < toInsert.size(); i += SAVE_CHUNK_SIZE) {
            int end = Math.min(i + SAVE_CHUNK_SIZE, toInsert.size());
            try {
            apartmentSubwayDistanceRepository.saveAll(toInsert.subList(i, end));
                if ((i / SAVE_CHUNK_SIZE) % 10 == 0) {
                    log.info("저장 진행: {} ~ {} / {}", i, end, toInsert.size());
                }
            } catch (Exception e) {
                // 저장 chunk 단위로 어디에서 터졌는지 남기기
                ApartmentSubwayDistance first = toInsert.get(i);
                ApartmentSubwayDistance last = toInsert.get(end - 1);
                log.error("아파트–지하철 거리 저장 중 예외 발생. chunkStart={}, chunkEnd={}, total={}, first(aptId={}, stationId={}, distKm={}), last(aptId={}, stationId={}, distKm={})",
                        i, end, toInsert.size(),
                        first.getApartment().getId(), first.getSubwayStation().getId(), first.getDistanceKm(),
                        last.getApartment().getId(), last.getSubwayStation().getId(), last.getDistanceKm(),
                        e);
                throw e;
            }
        }

        log.info("아파트–지하철 거리 적재 완료: {}건 (아파트 {}개, 역 {}개)", toInsert.size(), apartments.size(), stations.size());
        return RepeatStatus.FINISHED;
    }
}
