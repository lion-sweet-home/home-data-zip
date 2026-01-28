package org.example.homedatazip.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.data.Region;

@Entity
@Getter
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
}