package org.example.homedatazip.tradeRent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.apartment.entity.Apartment;

import java.time.LocalDate;

@Entity
@Getter
@Table(
        indexes = {
                @Index(
                        name = "idx_trade_rent_apt_date",
                        columnList = "apartment_id, dealDate"
                )
        }
)
public class TradeRent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Apartment apartment;

    private Integer deposit;
    private Integer monthlyRent;

    private Double exclusiveArea;
    private Integer floor;

    private LocalDate dealDate;          // 계약 년도

    private Boolean renewalRequested;    // 계약 구분
    private String rentTerm;             // 계약 기간

    private String sggCd;                // 지역 코드

}