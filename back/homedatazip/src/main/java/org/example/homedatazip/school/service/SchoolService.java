package org.example.homedatazip.school.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.entity.ApartmentSchoolDistance;
import org.example.homedatazip.apartment.repository.ApartmentSchoolDistanceRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SchoolErrorCode;
import org.example.homedatazip.school.dto.ApartmentNearSchoolResponse;
import org.example.homedatazip.school.dto.SchoolResponse;
import org.example.homedatazip.school.dto.SchoolSearchRequest;
import org.example.homedatazip.school.entity.School;
import org.example.homedatazip.school.repository.SchoolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private static final Set<Double> ALLOWED_RADIUS_KM = Set.of(0.5, 1.0, 2.0, 3.0, 5.0, 10.0);

    private final SchoolRepository schoolRepository;
    private final ApartmentSchoolDistanceRepository apartmentSchoolDistanceRepository;

    /** 시도·구군(필수), 동(옵션)으로 학교 목록 조회. 시도·구군 없으면 빈 목록 */
    @Transactional(readOnly = true)
    public List<SchoolResponse> searchSchoolsByRegion(SchoolSearchRequest request) {
        if (!StringUtils.hasText(request.sido()) || !StringUtils.hasText(request.gugun())) {
            return List.of();
        }
        String dongOptional = StringUtils.hasText(request.dong()) ? request.dong() : null;

        List<School> schools = schoolRepository.findByRegionSidoAndGugunAndDongOptional(
                request.sido(), request.gugun(), dongOptional);

        return schools.stream()
                .map(SchoolResponse::from)
                .toList();
    }

    /** 학교 반경(km) 이내 아파트 목록 (거리 오름차순) */
    @Transactional(readOnly = true)
    public List<ApartmentNearSchoolResponse> findApartmentsNearSchool(Long schoolId, double distanceKm) {
        if (!ALLOWED_RADIUS_KM.contains(distanceKm)) {
            throw new BusinessException(SchoolErrorCode.INVALID_RADIUS);
        }
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException(SchoolErrorCode.SCHOOL_NOT_FOUND));

        List<ApartmentSchoolDistance> distances = apartmentSchoolDistanceRepository
                .findBySchoolIdAndDistanceKmLessThanEqualOrderByDistanceKmAscWithApartment(schoolId, distanceKm);

        return distances.stream()
                .map(this::toApartmentNearSchoolResponse)
                .toList();
    }

    private ApartmentNearSchoolResponse toApartmentNearSchoolResponse(ApartmentSchoolDistance asd) {
        Apartment apt = asd.getApartment();
        return new ApartmentNearSchoolResponse(
                apt.getId(),
                apt.getAptName(),
                apt.getRoadAddress(),
                apt.getJibunAddress(),
                apt.getLatitude(),
                apt.getLongitude(),
                apt.getBuildYear(),
                asd.getDistanceKm()
        );
    }
}