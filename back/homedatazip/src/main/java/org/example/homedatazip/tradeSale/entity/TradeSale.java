package org.example.homedatazip.tradeSale.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.apartment.entity.Apartment;

import java.time.LocalDate;

@Entity
@Getter
@Table(
        indexes = {
                @Index(
                        name = "idx_trade_sale_apt_date",
                        columnList = "apartment_id, dealDate"
                )
        }
)
public class TradeSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Apartment apartment;

    private Integer dealAmount;

    private Double exclusiveArea;
    private Integer floor;

    private String aptDong;

    private LocalDate dealDate;          // 계약 년도

    private Boolean canceled;
    private LocalDate canceledDate;

    private String sggCd;                // 지역 코드

}