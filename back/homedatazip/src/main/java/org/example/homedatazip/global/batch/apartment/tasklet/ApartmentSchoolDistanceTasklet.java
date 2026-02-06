package org.example.homedatazip.global.batch.apartment.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.entity.ApartmentSchoolDistance;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.apartment.repository.ApartmentSchoolDistanceRepository;
import org.example.homedatazip.global.util.HaversineUtils;
import org.example.homedatazip.school.entity.School;
import org.example.homedatazip.school.repository.SchoolRepository;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 아파트–학교 거리(하버사인) 계산 후 10km 이내만 apartment_school_distances 에 적재.
 * 기존 데이터는 전부 삭제 후 재적재(Full Refresh).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApartmentSchoolDistanceTasklet implements Tasklet {

    private static final double MAX_RADIUS_KM = 10.0;
    private static final int SAVE_CHUNK_SIZE = 1000;

    private final ApartmentRepository apartmentRepository;
    private final SchoolRepository schoolRepository;
    private final ApartmentSchoolDistanceRepository apartmentSchoolDistanceRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<Apartment> apartments = apartmentRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull();
        List<School> schools = schoolRepository.findAll().stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .toList();

        if (apartments.isEmpty() || schools.isEmpty()) {
            log.warn("아파트 또는 학교가 없어 거리 계산을 건너뜁니다. apartments={}, schools={}",
                    apartments.size(), schools.size());
            return RepeatStatus.FINISHED;
        }

        apartmentSchoolDistanceRepository.deleteAllInBatch();

        List<ApartmentSchoolDistance> toInsert = new ArrayList<>();
        for (Apartment apt : apartments) {
            for (School school : schools) {
                double distanceKm = HaversineUtils.distanceKm(
                        apt.getLatitude(), apt.getLongitude(),
                        school.getLatitude(), school.getLongitude());
                if (distanceKm <= MAX_RADIUS_KM) {
                    toInsert.add(ApartmentSchoolDistance.of(apt, school, distanceKm));
                }
            }
        }

        for (int i = 0; i < toInsert.size(); i += SAVE_CHUNK_SIZE) {
            int end = Math.min(i + SAVE_CHUNK_SIZE, toInsert.size());
            apartmentSchoolDistanceRepository.saveAll(toInsert.subList(i, end));
        }

        log.info("아파트–학교 거리 적재 완료: {}건 (아파트 {}개, 학교 {}개)", toInsert.size(), apartments.size(), schools.size());
        return RepeatStatus.FINISHED;
    }
}