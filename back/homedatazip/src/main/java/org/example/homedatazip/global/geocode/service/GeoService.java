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

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GeoService {

    private final KakaoApiClient kakaoApiClient;
    private final RegionRepository regionRepository;

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
