package org.example.homedatazip.recommend.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import org.example.homedatazip.apartment.entity.Apartment;

@Builder
public record ApartmentResponse(
        Long id,
        String aptName,
        String roadAddress,
        String jibunAddress,
        Integer buildYear,
        Double latitude,
        Double longitude,
        String regionName,
        Double recommendArea,
        Long recommendPrice,
        Long recommendMonthly,
        String tradeType
) {

    @QueryProjection
    public ApartmentResponse(Long id, String aptName, String roadAddress, String jibunAddress,
                             Integer buildYear, Double latitude, Double longitude,
                             String regionName, Double recommendArea, Long recommendPrice,
                             Long recommendMonthly, String tradeType) {
        this.id = id;
        this.aptName = aptName;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.buildYear = buildYear;
        this.latitude = latitude;
        this.longitude = longitude;
        this.regionName = regionName;
        this.recommendArea = recommendArea;
        this.recommendPrice = recommendPrice;
        this.recommendMonthly = recommendMonthly;
        this.tradeType = tradeType;
    }

    public static ApartmentResponse of(Apartment apartment, Double area, Long price, Long monthlyRent, String tradeType) {
        String fullRegionName = (apartment.getRegion() != null)
                ? String.format("%s %s %s",
                apartment.getRegion().getSido(),
                apartment.getRegion().getGugun(),
                apartment.getRegion().getDong())
                : "지역 정보 없음";

        return ApartmentResponse.builder()
                .id(apartment.getId())
                .aptName(apartment.getAptName())
                .roadAddress(apartment.getRoadAddress())
                .jibunAddress(apartment.getJibunAddress())
                .buildYear(apartment.getBuildYear())
                .latitude(apartment.getLatitude())
                .longitude(apartment.getLongitude())
                .regionName(fullRegionName)
                .recommendArea(area)
                .recommendPrice(price)
                .recommendMonthly(monthlyRent)
                .tradeType(tradeType)
                .build();
    }
}