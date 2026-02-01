package org.example.homedatazip.subway.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.homedatazip.common.BaseTimeEntity;

@Entity
@Getter
@Setter
@Table(name = "subway_station_sources",
        indexes = {
                @Index(name = "idx_source_station", columnList = "station_id"),
                @Index(name = "idx_source_line", columnList = "lineName")
        })
public class SubwayStationSource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String lineStationCode;     // outStnNum → 노선별 역코드

    @Column(nullable = false)
    private String stationName;         // stnKrNm → 역명

    @Column(nullable = false)
    private String lineName;            // lineNm → 노선명

    @Column(nullable = false)
    private Double latitude;            // convY → 위도

    @Column(nullable = false)
    private Double longitude;           // convX → 경도

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private SubwayStation station;      // Step2 대표 역 매핑
}
