package org.example.homedatazip.settlement.repository;

import org.example.homedatazip.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);

    List<Settlement> findByPeriodStartBetweenOrderByPeriodStartAsc(LocalDate periodStartAfter, LocalDate periodStartBefore);
}
