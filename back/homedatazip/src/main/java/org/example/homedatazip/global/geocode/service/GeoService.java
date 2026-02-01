package org.example.homedatazip.global.geocode.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.data.repository.RegionRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.GeoErrorCode;
import org.example.homedatazip.global.geocode.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GeoService {

    private final KakaoApiClient kakaoApiClient;
    private final RegionRepository regionRepository;

    public CoordinateInfoResponse convertCoordinateInfo(
            String dong, String jibun, String sggCode, String apartmentName,
            String roadNm, String roadNmBonbun, String roadNmBubun) {

        // [개선 1] API 초당 호출 제한 방지
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 1. 지역 정보 조회 및 기본 검증
        Region region = regionRepository.findBySggCode(sggCode)
                .stream().findFirst().orElse(null);

        if (region == null) {
            log.warn(">>> [SKIPPED] 지역 정보(Region)를 찾을 수 없습니다. sggCode={}", sggCode);
            return null;
        }

        String safeSido = (region.getSido() != null) ? region.getSido() : "";
        String safeGugun = (region.getGugun() != null) ? region.getGugun() : "";
        if (safeSido.isEmpty() || safeGugun.isEmpty()) return null;

        // 2. 검색용 주소 문자열 미리 생성
        String roadAddressSearch = buildRoadAddress(safeSido, safeGugun, roadNm, roadNmBonbun, roadNmBubun);
        String jibunAddressSearch = buildJibunAddress(safeSido, safeGugun, dong, jibun, region);

        try {
            GeoCoordinateResponse response = null;
            String method = "";

            // [STEP 1] 도로명 주소 검색 (최우선)
            if (roadAddressSearch != null) {
                response = kakaoApiClient.getCoordinateByAddress(roadAddressSearch);
                if (!isEmpty(response)) method = "도로명";
            }

            // [STEP 2] 도로명 실패 시 지번 주소 검색 (Fallback 1)
            if (isEmpty(response)) {
                response = kakaoApiClient.getCoordinateByAddress(jibunAddressSearch);
                if (!isEmpty(response)) method = "지번";
            }

            // [STEP 3] 모두 실패 시 키워드(아파트명) 검색 (Fallback 2)
            if (isEmpty(response)) {
                String keyword = safeSido + " " + safeGugun + " " + apartmentName;
                response = kakaoApiClient.getCoordinateByKeyword(keyword);
                if (!isEmpty(response)) method = "키워드";
            }

            // 결과 검증 로그
            if (isEmpty(response)) {
                log.warn(">>> [FAILED] 좌표 찾기 실패: {} (지번주소: {})", apartmentName, jibunAddressSearch);
                return null;
            }

            log.info(">>> [SUCCESS] 좌표 검색 성공 ({}): {}", method, apartmentName);

            // 3. 결과 데이터 추출
            GeoCoordinateResponse.Document document = response.documents().getFirst();
            Double latitude = extractLatitude(document);
            Double longitude = extractLongitude(document);

            // 4. 도로명 주소 텍스트 보정 (결과에 없으면 생성된 값 사용)
            String finalRoadAddress = (document.roadAddress() != null)
                    ? document.roadAddress().roadAddressName() : roadAddressSearch;

            String finalJibunAddress = (document.address() != null)
                    ? document.address().addressName() : jibunAddressSearch;

            return CoordinateInfoResponse.create(region, finalJibunAddress, finalRoadAddress, latitude, longitude);

        } catch (Exception e) {
            handleApiException(e, apartmentName);
            return null;
        }
    }

// --- 헬퍼 메소드 (로직 분리로 가독성 향상) ---

    private String buildRoadAddress(String sido, String gugun, String roadNm, String bonbun, String bubun) {
        if (roadNm == null || roadNm.trim().isEmpty()) return null;
        StringBuilder sb = new StringBuilder(sido).append(" ").append(gugun).append(" ").append(roadNm.trim());
        if (bonbun != null && !bonbun.isEmpty()) {
            sb.append(" ").append(bonbun.replaceAll("^0+", ""));
            if (bubun != null && !bubun.isEmpty() && !bubun.equals("00000")) {
                sb.append("-").append(bubun.replaceAll("^0+", ""));
            }
        }
        return sb.toString();
    }

    private String buildJibunAddress(String sido, String gugun, String dong, String jibun, Region region) {
        String realDong = (dong != null && !dong.trim().isEmpty() && !dong.equalsIgnoreCase("null"))
                ? dong.trim() : (region.getDong() != null ? region.getDong() : "");

        String cleanJibun = "";
        if (jibun != null && !jibun.trim().isEmpty()) {
            cleanJibun = jibun.replaceAll("[^0-9-]", " ").trim();
            if (cleanJibun.contains(" ")) {
                String[] parts = cleanJibun.split("\\s+");
                cleanJibun = parts[parts.length - 1];
            }
            cleanJibun = cleanJibun.replaceAll("^0+", "");
        }

        return Stream.of(sido, gugun, realDong, cleanJibun)
                .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("null"))
                .collect(Collectors.joining(" ")).trim();
    }

    private Double extractLatitude(GeoCoordinateResponse.Document doc) {
        if (doc.roadAddress() != null && doc.roadAddress().latitude() != null) return doc.roadAddress().latitude();
        if (doc.address() != null && doc.address().latitude() != null) return doc.address().latitude();
        return doc.y();
    }

    private Double extractLongitude(GeoCoordinateResponse.Document doc) {
        if (doc.roadAddress() != null && doc.roadAddress().longitude() != null) return doc.roadAddress().longitude();
        if (doc.address() != null && doc.address().longitude() != null) return doc.address().longitude();
        return doc.x();
    }

    private boolean isEmpty(GeoCoordinateResponse response) {
        return response == null || response.documents() == null || response.documents().isEmpty();
    }

    private void handleApiException(Exception e, String apartmentName) {
        if (e instanceof RestClientResponseException re && re.getStatusCode().value() == 429) {
            log.error(">>> [API LIMIT] 429 Too Many Requests (RestClient). 아파트: {}", apartmentName);
        } else if (e instanceof WebClientResponseException we && we.getStatusCode().value() == 429) {
            log.error(">>> [API LIMIT] 429 Too Many Requests (WebClient). 아파트: {}", apartmentName);
        } else {
            log.error(">>> [ERROR] 주소 변환 중 예외 발생: {}", e.getMessage());
        }
    }

    // 주소로 좌표변환
    public CoordinateInfoResponse convertCoordinateInfo(String dong, String jibun) {

        String dongJibun = dong + " " + jibun;

        GeoCoordinateResponse response = kakaoApiClient.getCoordinateByAddress(dongJibun);

        // 검색결과가 null인경우
        if (response == null || response.documents().isEmpty()) {
            log.error("API 요청 결과가 존재하지 않습니다. dongJibun={}", dongJibun);
            throw new BusinessException(GeoErrorCode.RESPONSE_NOT_FOUND);
        }

        // 동 + 지번 조합이라고 무조건 한 개의 데이터가 보장되는 것은 아니기 때문에 list로 받아야한다.
        GeoCoordinateResponse.Document document = response.documents().getFirst();

        // 지번주소 정보
        GeoCoordinateResponse.Address address = document.address();

        // 도로명주소 정보
        GeoCoordinateResponse.RoadAddress roadAddress = document.roadAddress();

        // 지번주소의 위경도값과 도로명주소의 위경도값이 살짝 다른데,
        // 지번주소는 토지의 가운데 점 기준, 도로명주소는 실제 출입구를 기준으로 계산이 되기때문.
        // 정확한 주소는 도로명주소의 위경도 값이라고함, null일 경우 지번주소의 위경도값을 사용.
        Double latitude = roadAddress != null
                ? roadAddress.latitude() : address.latitude();
        Double longitude = roadAddress != null
                ? roadAddress.longitude() : address.longitude();
        String roadAddressName = roadAddress != null
                ? roadAddress.roadAddressName() : null;
        if (roadAddress == null) {
            log.info("도로명 주소가 없어 지번 주소를 사용 - dongJibun={}", dongJibun);
        }

        // 지역 조회
        Region region = regionRepository.findByLawdCode(address.bCode())
                .orElseThrow(() -> {
                    log.error("지역을 찾을 수 없습니다. bCode={}", address.bCode());
                    return new RuntimeException("지역을 찾을 수 없습니다");
                }); // todo: 나중에 에러코드로 변경

        return CoordinateInfoResponse.create(
                region,
                address.addressName(),
                roadAddressName,
                latitude,
                longitude);

    }

    // 좌표로 주소변환
    public Region convertAddressInfo(Double latitude, Double longitude) {

        GeoAddressResponse response = kakaoApiClient.getAddressByCoordinate(latitude, longitude);

        // 검색결과가 null인경우
        if (response == null || response.documents().isEmpty()) {
            log.error("API 요청 결과가 존재하지 않습니다. latitude={}, longitude={}", latitude, longitude);
            throw new BusinessException(GeoErrorCode.RESPONSE_NOT_FOUND);
        }

        // 법정동코드 가져오기
        String bCode = response.documents().stream()
                .filter(document -> "B".equals(document.regionType()))
                .map(GeoAddressResponse.Document::bCode)
                .findFirst()
                .orElseThrow(() -> {
                    log.error("주소 변환 중 에러 발생! latitude={}, longitude={}", latitude, longitude);
                    return new BusinessException(GeoErrorCode.CONVERT_COORDINATE_ERROR);
                });

        log.info("법정동 코드 추출 - bCode={}, regionName={}", bCode, response.documents().getFirst().addressName());

        // 지역 조회
        return regionRepository.findByLawdCode(bCode)
                .orElseThrow(() -> {
                    log.error("지역을 찾을 수 없습니다. bCode={}", bCode);
                    return new RuntimeException("지역을 찾을 수 없습니다");
                });// todo: 나중에 에러코드로 변경
    }

}
