package org.example.homedatazip.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.data.Region;

@Entity
@Getter
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

    @ManyToOne
    private Region region;
}
