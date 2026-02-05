package org.example.homedatazip.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homedatazip.data.Region;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "school")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String schoolId;
    private String name;
    private String schoolLevel;       // 초/중/고 구분
    private String roadAddress;       // 소재지 도로명 주소

    private Double latitude;
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    public static School from(
            String schoolId,
            String name,
            String schoolLevel,
            String roadAddress,
            Double latitude,
            Double longitude,
            Region region
    ) {
        return School.builder()
                .schoolId(schoolId)
                .name(name)
                .schoolLevel(schoolLevel)
                .roadAddress(roadAddress)
                .latitude(latitude)
                .longitude(longitude)
                .region(region)
                .build();
    }
}
// 지하철이랑 똑같이 아파트-학교 거리 하기 < 앞으로 할일,,,
