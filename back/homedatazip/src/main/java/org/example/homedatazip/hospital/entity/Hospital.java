package org.example.homedatazip.hospital.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.homedatazip.data.Region;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String hospitalId; // 기관 ID
    private String name;    // 기관명

    private String typeName;   // 병원 분류명 (의원, 종합병원 등)

    private Double latitude; // 위도
    private Double longitude; // 경도

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region; // 지역
    private String address; // 상세 주소

    private String gu;
    private String dong;

    /**
     * API 응답 -> Entity
     */
    public static Hospital fromApiResponse(
            String hospitalId,
            String name,
            String typeName,
            Region region,
            String address,
            Double latitude,
            Double longitude
    ) {
        // 주소에서 구/동 추출
        String gu = extractGu(address);
        String dong = extractDong(address);

        return Hospital.builder()
                .hospitalId(hospitalId)
                .name(name)
                .typeName(typeName)
                .region(region)
                .address(address)
                .gu(gu)
                .dong(dong)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    /**
     * 기존 엔티티 업데이트
     */
    public void updateFrom(
            String name,
            String typeName,
            Region region,
            String address,
            Double latitude,
            Double longitude
    ) {
        this.name = name;
        this.typeName = typeName;
        this.region = region;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.gu = extractGu(address);
        this.dong = extractDong(address);
    }

    /**
     * 주소에서 '구' 추출
     */
    private static String extractGu(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }

        String[] parts = address.split(" ");

        for (String part : parts) {
            if (part.endsWith("구")) {
                return part;
            }
        }
        return null;
    }

    /**
     * 주소에서 '동' 추출
     */
    private static String extractDong(String address) {
        if (address == null || address.isEmpty()) {
            return null;
        }

        // 괄호 위치 찾기
        int startIndex = address.indexOf("(");
        int endIndex = address.indexOf(")");

        // 괄호가 존재한다면 (__동, __)
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            String dongInfo = address.substring(startIndex + 1, endIndex);

            // ["__동", "__"]
            String[] parts = dongInfo.split(",");

            for (String part : parts) {
                part = part.trim();

                if (part.endsWith("동")) {
                    return part;
                }
            }
        }
        return null;
    }
}