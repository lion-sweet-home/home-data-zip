package org.example.homedatazip.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * OpenAPI 동기화용 학교 원천 테이블.
 * 배치에서 INSERT/UPDATE하는 테이블이므로 JPA로 스키마가 생성되도록 엔티티만 정의.
 */
@Entity
@Getter
@Table(name = "school_sources")
public class SchoolSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", unique = true, nullable = false)
    private String schoolId;

    @Column(nullable = false)
    private String name;

    private String schoolLevel;
    private String roadAddress;
    private String jibunAddress;
    private Double latitude;
    private Double longitude;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}