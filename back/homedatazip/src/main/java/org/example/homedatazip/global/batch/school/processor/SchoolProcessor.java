package org.example.homedatazip.global.batch.school.processor;
import org.example.homedatazip.school.dto.SchoolSourceSync;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class SchoolProcessor implements ItemProcessor<SchoolSourceSync, SchoolSourceSync> {

    @Override
    public SchoolSourceSync process(SchoolSourceSync item) {
        if (item == null) return null;

        // 학교 필수 데이터 검증 (학교ID, 학교명, 주소, 좌표 등이 없으면 저장 안 함)
        if (trimToNull(item.schoolId()) == null
                || trimToNull(item.schoolName()) == null
                || trimToNull(item.jibunAddress()) == null
                || item.latitude() == null
                || item.longitude() == null) {
            return null; // 스킵
        }
        return item; // 가공하지 않고 그대로 보냄
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}