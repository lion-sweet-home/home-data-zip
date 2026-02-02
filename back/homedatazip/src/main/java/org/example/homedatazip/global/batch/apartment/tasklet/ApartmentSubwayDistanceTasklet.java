package org.example.homedatazip.global.batch.apartment.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.entity.ApartmentSubwayStation;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.apartment.repository.ApartmentSubwayStationRepository;
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
 * 아파트–지하철역 거리(하버사인) 계산 후 10km 이내만 apartment_subway_stations 에 적재.
 * 기존 데이터는 전부 삭제 후 재적재(Full Refresh).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApartmentSubwayDistanceTasklet implements Tasklet {

    private static final double MAX_RADIUS_KM = 10.0;
    private static final int SAVE_CHUNK_SIZE = 500;

    private final ApartmentRepository apartmentRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final ApartmentSubwayStationRepository apartmentSubwayStationRepository;

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

        apartmentSubwayStationRepository.deleteAllInBatch();

        List<ApartmentSubwayStation> toInsert = new ArrayList<>();
        for (Apartment apt : apartments) {
            for (SubwayStation station : stations) {
                double distanceKm = HaversineUtils.distanceKm(
                        apt.getLatitude(), apt.getLongitude(),
                        station.getLatitude(), station.getLongitude());
                if (distanceKm <= MAX_RADIUS_KM) {
                    toInsert.add(ApartmentSubwayStation.of(apt, station, distanceKm));
                }
            }
        }

        for (int i = 0; i < toInsert.size(); i += SAVE_CHUNK_SIZE) {
            int end = Math.min(i + SAVE_CHUNK_SIZE, toInsert.size());
            apartmentSubwayStationRepository.saveAll(toInsert.subList(i, end));
        }

        log.info("아파트–지하철 거리 적재 완료: {}건 (아파트 {}개, 역 {}개)", toInsert.size(), apartments.size(), stations.size());
        return RepeatStatus.FINISHED;
    }
}
