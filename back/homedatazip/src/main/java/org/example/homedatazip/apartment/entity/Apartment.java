package org.example.homedatazip.apartment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.data.Region;

@Entity
@Getter
@Table(
        name = "apartments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_apt_seq", columnNames = "aptSeq")
        },
        indexes = {
                // 좌표 검색 성능을 위한 복합 인덱스 (반경 필터링 시 성능 발휘)
                @Index(name = "idx_apt_coords", columnList = "latitude, longitude"),
                // 주소 기반 조회를 위한 인덱스 (지오코딩 캐싱용)
                @Index(name = "idx_apt_road_address", columnList = "roadAddress")
        }
)
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aptName;
    private String roadAddress;
    private String jibunAddress;

    private Double latitude;
    private Double longitude;

    private Integer buildYear;

    @Column(nullable = false)
    private String aptSeq;

    @ManyToOne
    private Region region;

}