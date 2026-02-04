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
        return Hospital.builder()
                .hospitalId(hospitalId)
                .name(name)
                .typeName(typeName)
                .region(region)
                .address(address)
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
    }
}