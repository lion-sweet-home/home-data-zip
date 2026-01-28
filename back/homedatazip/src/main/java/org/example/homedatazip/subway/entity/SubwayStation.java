package org.example.homedatazip.subway.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class SubwayStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String bldnId;             // 역사_ID

    private String stationName;
    private String route;

    private Double latitude;
    private Double longitude;

}