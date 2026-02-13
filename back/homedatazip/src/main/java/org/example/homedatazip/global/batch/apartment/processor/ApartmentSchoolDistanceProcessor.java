package org.example.homedatazip.global.batch.apartment.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.entity.ApartmentSchoolDistance;
import org.example.homedatazip.global.util.HaversineUtils;
import org.example.homedatazip.school.entity.School;
import org.example.homedatazip.school.repository.SchoolRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 아파트 1건 × 전체 학교 → 10km 이내 ApartmentSchoolDistance 목록으로 변환.
 * 학교 목록은 Step 실행 시 1회 로드 후 캐시.
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class ApartmentSchoolDistanceProcessor implements ItemProcessor<Apartment, List<ApartmentSchoolDistance>> {

    private static final double MAX_RADIUS_KM = 10.0;
    private static final long PROGRESS_LOG_EVERY_APT = 200;

    private final SchoolRepository schoolRepository;

    private volatile List<School> schoolsCache;
    private long processedCount = 0;

    @Override
    public List<ApartmentSchoolDistance> process(Apartment apartment) {
        processedCount++;
        if (processedCount % PROGRESS_LOG_EVERY_APT == 0) {
            log.info("[아파트-학교 거리] 진행률: processed={} (current aptId={}, aptName={})",
                    processedCount, apartment.getId(), apartment.getAptName());
        }

        List<School> schools = getSchools();
        if (schools.isEmpty()) {
            return List.of();
        }

        try {
            List<ApartmentSchoolDistance> result = new ArrayList<>();
            double aptLat = apartment.getLatitude();
            double aptLon = apartment.getLongitude();

            for (School school : schools) {
                double distanceKm = HaversineUtils.distanceKm(
                        aptLat, aptLon,
                        school.getLatitude(), school.getLongitude());
                if (distanceKm <= MAX_RADIUS_KM) {
                    result.add(ApartmentSchoolDistance.of(apartment, school, distanceKm));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[아파트-학교 거리] 거리 계산 중 예외 발생. aptId={}, aptName={}, lat={}, lon={}, schoolsSize={}",
                    apartment.getId(), apartment.getAptName(), apartment.getLatitude(), apartment.getLongitude(),
                    schools.size(), e);
            throw e;
        }
    }

    private List<School> getSchools() {
        if (schoolsCache == null) {
            synchronized (this) {
                if (schoolsCache == null) {
                    schoolsCache = schoolRepository.findAll().stream()
                            .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                            .toList();
                    log.info("[아파트-학교 거리] 학교 {}개 로드 완료", schoolsCache.size());
                }
            }
        }
        return schoolsCache;
    }
}
