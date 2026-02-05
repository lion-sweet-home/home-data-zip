package org.example.homedatazip.global.batch.school.processor;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.exception.BatchSkipException;
import org.example.homedatazip.global.geocode.service.GeoService;
import org.example.homedatazip.school.dto.SchoolSourceSync;
import org.example.homedatazip.school.entity.School;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchoolProcessor implements ItemProcessor<SchoolSourceSync, School> {

    private final GeoService geoService;

    @Override
    public School process(SchoolSourceSync item) {
        if (item == null) return null;

        // 학교 필수 데이터 검증 (학교ID, 학교명, 주소, 좌표 등이 없으면 저장 안 함)
        if (trimToNull(item.schoolId()) == null
                || trimToNull(item.schoolName()) == null
                || trimToNull(item.roadAddress()) == null
                || item.latitude() == null
                || item.longitude() == null) {
            return null; // 스킵
        }

        if (!isTargetRegion(item.roadAddress())) return null;

        // Region 매칭
        Region region = null;
        try {
             region = geoService.convertAddressInfoInNewTransaction(item.latitude(), item.longitude());
        } catch (BatchSkipException e) {
            System.out.println("Region 조회 실패: " + e.getMessage());
            return null;
        }

        return School.
                from(
                        item.schoolId(),
                        item.schoolName(),
                        item.schoolLevel(),
                        item.roadAddress(),
                        item.latitude(),
                        item.longitude(),
                        region
                ); // 가공하지 않고 그대로 보냄
    }

    private boolean isTargetRegion(String roadAddress) {
        if (roadAddress == null || roadAddress.isBlank()) return false;

        return roadAddress.startsWith("서울") ||
                roadAddress.startsWith("인천") ||
                roadAddress.startsWith("경기");
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}