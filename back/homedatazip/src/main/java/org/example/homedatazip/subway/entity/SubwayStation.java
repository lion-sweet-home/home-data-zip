package org.example.homedatazip.subway.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.data.Region;

@Entity
@Getter
@Setter
@Table(name = "subway_stations")
public class SubwayStation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable=false)
    private String stationName;

    private Double latitude;   // 대표 좌표 (지도 마커용)
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;     // 역지오코딩 결과 (대표 위/경도 → 법정동)
}