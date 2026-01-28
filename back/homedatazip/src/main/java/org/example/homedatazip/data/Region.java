package org.example.homedatazip.data;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "lawdCode")
        }
)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sido;
    private String gugun;
    private String dong;

    @Column(nullable = false)
    private String lawdCode;

}
