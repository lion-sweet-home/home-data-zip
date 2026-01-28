package org.example.homedatazip.bus.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.data.Region;

@Entity
@Getter
public class BusStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String nodeId;

    @Column(unique = true)
    private String stationNumber; //제공하는 데이터의 타입이 어떤건줄 몰라서 일단은 String으로 적었어요
    private String name;

    private String address;

    private Double latitude;
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;
}
