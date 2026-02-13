package org.example.homedatazip.apartment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.homedatazip.school.entity.School;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "apartment_school_distances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_apt_school", columnNames = {"apartment_id", "school_id"})
        },
        indexes = {
                // 아파트, 학교 기준 반경 검색 쿼리 인덱스
                @Index(name = "idx_apt_school_apt_distance", columnList = "apartment_id, distance_km"),
                @Index(name = "idx_apt_school_distance", columnList = "school_id, distance_km")
        }
)
public class ApartmentSchoolDistance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false, name = "distance_km")
    private Double distanceKm;

    private ApartmentSchoolDistance(Apartment apartment, School school, Double distanceKm) {
        this.apartment = apartment;
        this.school = school;
        this.distanceKm = distanceKm;
    }

    public static ApartmentSchoolDistance of(Apartment apartment, School school, double distanceKm) {
        return new ApartmentSchoolDistance(apartment, school, distanceKm);
    }

    public void updateDistance(double distanceKm) {
        this.distanceKm = distanceKm;
    }
}
