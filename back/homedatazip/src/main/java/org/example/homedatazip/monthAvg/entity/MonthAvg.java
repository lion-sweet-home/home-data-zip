package org.example.homedatazip.monthAvg.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "month_avg",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_month_avg",
                columnNames = {"apt_id", "yyyymm", "area_type_id"}
        ),
        indexes = {
                @Index(name = "idx_month_avg_apt_yyyymm", columnList = "apt_id, yyyymm")
        }
)
public class MonthAvg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "month_avg_id")
    private Long id;

    @Column(name = "apt_id", nullable = false)
    private Long aptId;

    @Column(name = "yyyymm", nullable = false, length = 6)
    private String yyyymm;

    @Column(name = "area_type_id", nullable = false)
    private Long areaTypeId;

    @Column(name = "sale_deal_amount_sum", nullable = false)
    private Long saleDealAmountSum;

    @Column(name = "jeonse_deposit_sum", nullable = false)
    private Long jeonseDepositSum;

    @Column(name = "wolse_deposit_sum", nullable = false)
    private Long wolseDepositSum;

    @Column(name = "wolse_rent_sum", nullable = false)
    private Long wolseRentSum;

    @Column(name = "sale_count", nullable = false)
    private Integer saleCount;

    @Column(name = "jeonse_count", nullable = false)
    private Integer jeonseCount;

    @Column(name = "wolse_count", nullable = false)
    private Integer wolseCount;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;


}
