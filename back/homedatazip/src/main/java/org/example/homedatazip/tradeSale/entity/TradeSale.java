package org.example.homedatazip.tradeSale.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.tradeSale.dto.ApartmentTradeSaleItem;

import java.time.LocalDate;

@Entity
@Getter
@Table(
        name = "trade_sale",
        indexes = {
                @Index(
                        name = "idx_trade_sale_apt_date",
                        columnList = "apartment_id, deal_date"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Apartment apartment;

    private Long dealAmount;

    private Double exclusiveArea;
    private Integer floor;

    private String aptDong;

    private LocalDate dealDate;          // 계약 년도

    private Boolean canceled;
    private LocalDate canceledDate;

    private String sggCd;                // 지역 코드

    public TradeSale(Apartment apartment, Long dealAmount, Double exclusiveArea,
                     Integer floor, String aptDong, LocalDate dealDate,
                     String sggCd, Boolean canceled) {
        this.apartment = apartment;
        this.dealAmount = dealAmount;
        this.exclusiveArea = exclusiveArea;
        this.floor = floor;
        this.aptDong = aptDong;
        this.dealDate = dealDate;
        this.sggCd = sggCd;
        this.canceled = canceled;
    }

    public static TradeSale from(ApartmentTradeSaleItem item, Apartment apartment) {
        return new TradeSale(
                apartment,
                Long.parseLong(item.getDealAmount().trim().replace(",", "")),
                Double.parseDouble(item.getExcluUseAr().trim()),
                Integer.parseInt(item.getFloor().trim()),
                item.getAptDong(),
                LocalDate.of(
                        Integer.parseInt(item.getDealYear().trim()),
                        Integer.parseInt(item.getDealMonth().trim()),
                        Integer.parseInt(item.getDealDay().trim())
                ),
                item.getSggCd(),
                (item.getCdealType() != null && !item.getCdealType().isBlank())
        );
    }

}