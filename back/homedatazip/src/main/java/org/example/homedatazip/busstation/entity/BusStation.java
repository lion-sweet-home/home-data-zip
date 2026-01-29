package org.example.homedatazip.busstation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.homedatazip.data.Region;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "bus_station",
        indexes = {
                @Index(name = "idx_bus_station_station_number", columnList = "station_number"),
                @Index(name = "idx_bus_station_region", columnList = "region_id"),
                @Index(name = "idx_bus_station_name", columnList = "name")
        }
)
public class BusStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 정류소번호
    @Column(name = "node_id", nullable = false, unique = true, length = 20)
    private String nodeId;

    // STOPS_NO : ARS-ID
    @Column(name = "station_number", length = 10)
    private String stationNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String address;

    private Double latitude; //YCRD 경도
    private Double longitude; //XCRD 위도

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    public BusStation(String nodeId) {
        this.nodeId = nodeId;
    }

    public void update(
            String stationNumber,
            String name,
            Double longitude,
            Double latitude,
            Region region
    ) {
        this.stationNumber = stationNumber;
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.region = region;
    }
}
