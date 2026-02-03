package org.example.homedatazip.global.batch.school.reader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.school.dto.SchoolSourceSync;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchoolApiReader implements ItemReader<SchoolSourceSync> {

    @Value("${DATA_GO_KR_SERVICE_KEY}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private Iterator<SchoolSourceSync> itemIterator;

    @Override
    public SchoolSourceSync read() {
        if (itemIterator == null) {
            List<SchoolSourceSync> items = fetchSchoolsFromApi();
            itemIterator = items.iterator();
        }
        return itemIterator.hasNext() ? itemIterator.next() : null;
    }

    private List<SchoolSourceSync> fetchSchoolsFromApi() {
        log.info("학교 OpenAPI 데이터 호출 시작...");
        List<SchoolSourceSync> schoolList = new ArrayList<>();

        String url = "http://api.data.go.kr/openapi/tn_pubr_public_schul_inst_api?serviceKey="
                + serviceKey + "&type=json&numOfRows=100";

        try {
            // 1. API 호출
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // 2. JSON 구조 파헤쳐서 실제 학교 리스트(items) 꺼내기
            Map<String, Object> responseMap = (Map<String, Object>) response.get("response");
            Map<String, Object> bodyMap = (Map<String, Object>) responseMap.get("body");
            List<Map<String, String>> items = (List<Map<String, String>>) bodyMap.get("items");

            if (items != null) {
                for (Map<String, String> item : items) {
                    // 에러 해결: 요구하는 7개의 파라미터를 모두 채워줍니다.
                    SchoolSourceSync school = new SchoolSourceSync(
                            item.get("schulNm"),      // 1. 학교명 (String)
                            item.get("schulSeNm"),    // 2. 학교급구분 (String)
                            item.get("rdnmadr"),      // 3. 소재지도로명주소 (String)
                            item.get("atptOfcdcNm"),  // 4. 시도교육청명 (String)
                            item.get("juOfcdcNm"),    // 5. 시군구교육지원청명 (String)
                            Double.parseDouble(item.getOrDefault("latitude", "0.0")),  // 6. 위도 (Double)
                            Double.parseDouble(item.getOrDefault("longitude", "0.0")) // 7. 경도 (Double)
                    );
                    schoolList.add(school);
                }
                log.info("성공적으로 {}건의 학교 데이터를 변환하여 담았습니다!", schoolList.size());
            }
        } catch (Exception e) {
            log.error("API 호출 또는 데이터 변환 중 에러 발생: {}", e.getMessage());
            e.printStackTrace();
        }

        return schoolList;
    }
}