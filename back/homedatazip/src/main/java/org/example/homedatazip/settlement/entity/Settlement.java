package org.example.homedatazip.settlement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.common.BaseTimeEntity;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "settlements")
public class Settlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @Column(name = "period_start")
    private LocalDate periodStart;

    @Column(name = "period_end")
    private LocalDate periodEnd;

    private Long amount = 0L;

    public static Settlement createMonthly(LocalDate date) {
        Settlement settlement = new Settlement();
        settlement.periodStart = date.withDayOfMonth(1);
        settlement.periodEnd = date.withDayOfMonth(date.lengthOfMonth());
        settlement.amount = 0L;
        return settlement;
    }

    public void addAmount(Long amount) {
        this.amount += amount;
    }
}
