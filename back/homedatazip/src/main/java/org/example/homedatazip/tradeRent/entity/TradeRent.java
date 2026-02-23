package org.example.homedatazip.tradeRent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "trade_rent",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trade_rent_dedup",
                        columnNames = {"apartment_id", "deal_date", "exclusive_area", "floor", "deposit", "monthly_rent"}
                )
        },
        indexes = {
                @Index(name = "idx_trade_rent_apt_date", columnList = "apartment_id, deal_date")
        }
)
public class TradeRent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="apartment_id", nullable=false)
    private Apartment apartment;

    @Column(name = "deposit", nullable = false)
    private Long deposit;

    @Column(name = "monthly_rent", nullable = false)
    private Integer monthlyRent;

    @Column(name = "exclusive_area", nullable = false)
    private Double exclusiveArea;

    @Column(name = "floor", nullable = false)
    private Integer floor;

    @Column(name = "deal_date", nullable = false)
    private LocalDate dealDate;          // 계약 년도

    @Column(name = "renewal_requested")
    private Boolean renewalRequested;    // 계약 구분

    @Column(name = "rent_term", nullable = false)
    private String rentTerm;             // 계약 기간

    @Column(name = "sgg_code", nullable = false)
    private String sggCode;                // 지역 코드

}