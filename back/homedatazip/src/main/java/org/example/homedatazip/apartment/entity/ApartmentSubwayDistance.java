package org.example.homedatazip.apartment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.homedatazip.subway.entity.SubwayStation;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "apartment_subway_distances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_apt_subway", columnNames = {"apartment_id", "subway_station_id"})
        },
        indexes = {
                // 아파트, 역 기준 반경 검색 쿼리 인덱스
                @Index(name = "idx_apt_subway_apt_distance", columnList = "apartment_id, distance_km"),
                @Index(name = "idx_apt_subway_station_distance", columnList = "subway_station_id, distance_km")
        }
)
public class ApartmentSubwayDistance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subway_station_id", nullable = false)
    private SubwayStation subwayStation;

    @Column(nullable = false, name = "distance_km")
    private Double distanceKm;

    private ApartmentSubwayDistance(Apartment apartment, SubwayStation subwayStation, Double distanceKm) {
        this.apartment = apartment;
        this.subwayStation = subwayStation;
        this.distanceKm = distanceKm;
    }

    public static ApartmentSubwayDistance of(Apartment apartment, SubwayStation subwayStation, double distanceKm) {
        return new ApartmentSubwayDistance(apartment, subwayStation, distanceKm);
    }

    public void updateDistance(double distanceKm) {
        this.distanceKm = distanceKm;
    }
}
