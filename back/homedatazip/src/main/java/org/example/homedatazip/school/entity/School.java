package org.example.homedatazip.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.homedatazip.data.Region;

@Entity
@Getter
@Setter
@Table(name = "School")
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
// 주현님이 하는것처럼 아파트를 끌고 하버사인 으로 거리계산을 하고 중간테이블을 만들어라
// 하버사인은 구현이 안되어있으니 각자 개발

