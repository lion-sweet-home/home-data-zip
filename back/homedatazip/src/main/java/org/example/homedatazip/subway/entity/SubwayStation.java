package org.example.homedatazip.subway.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.homedatazip.common.BaseTimeEntity;

@Entity
@Getter
@Setter
@Table(name = "subway_stations")
public class SubwayStation extends BaseTimeEntity {

    // TODO: 일단 대표 위경도는 이따가 하고 openapi 로 소스코드 다 받아오면 그 때 할거임
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable=false)
    private String stationName;

    private Double latitude;   // 대표 좌표 (지도 마커용)
    private Double longitude;

}