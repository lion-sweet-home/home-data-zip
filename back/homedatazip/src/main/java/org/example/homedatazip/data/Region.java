package org.example.homedatazip.data;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.example.homedatazip.data.dto.RegionApiResponse;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "region",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_region_lawd_code",
                        columnNames = {"lawd_code"}
                )
        }
)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sido;
    private String gugun;
    private String dong;

    @Column(nullable = false, length = 10)
    private String lawdCode;

    @Column(nullable = false, length = 5)
    private String sggCode;

    private Region(String sido, String gugun, String dong, String lawdCode) {
        this.sido = sido;
        this.gugun = gugun;
        this.dong = dong;
        this.lawdCode = lawdCode;
        this.sggCode = extractSggCode(lawdCode);
    }

    // DTO로부터 엔티티 생성
    public static Region from(RegionApiResponse dto) {
        return new Region(
                dto.sido(),
                dto.gugun(),
                dto.dong(),
                dto.lawdCode()
        );
    }

    private static String extractSggCode(String lawdCode) {
        if (lawdCode == null || lawdCode.length() < 5) {
            throw new IllegalArgumentException("유효하지 않은 법정동 코드입니다.");
        }
        return lawdCode.substring(0, 5);
    }

    // 데이터 업데이트
    public void updateFrom(RegionApiResponse response) {
        this.sido = response.sido();
        this.gugun = response.gugun();
        this.dong = response.dong();
    }
}
