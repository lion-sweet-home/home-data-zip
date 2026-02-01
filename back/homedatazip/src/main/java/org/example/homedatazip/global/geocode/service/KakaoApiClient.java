package org.example.homedatazip.global.geocode.service;

import org.example.homedatazip.global.geocode.dto.GeoAddressResponse;
import org.example.homedatazip.global.geocode.dto.GeoCoordinateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoApiClient {

    private final RestClient restClient;

    public KakaoApiClient(@Value("${kakao.api.key}") String key,
                          @Value("${kakao.api.url}") String url)
    {
        this.restClient = RestClient.builder()
                .baseUrl(url)
                .defaultHeader("Authorization", "KakaoAK " + key)
                .build();
    }

    // 주소로 좌표 변환
    public GeoCoordinateResponse getCoordinateByAddress(String address) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/address.json")
                        .queryParam("query", address)
                        .build())
                .retrieve()
                .body(GeoCoordinateResponse.class);
    }

    // 좌표로 주소 변환
    public GeoAddressResponse getAddressByCoordinate(Double latitude, Double longitude) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/geo/coord2regioncode.json")
                        .queryParam("x", longitude) // 경도
                        .queryParam("y", latitude) // 위도
                        .build())
                .retrieve()
                .body(GeoAddressResponse.class);
    }

    // [추가] 키워드로 좌표 변환 (아파트 이름 검색용)
    public GeoCoordinateResponse getCoordinateByKeyword(String keyword) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", keyword)
                        .build())
                .retrieve()
                .body(GeoCoordinateResponse.class);
    }
}
