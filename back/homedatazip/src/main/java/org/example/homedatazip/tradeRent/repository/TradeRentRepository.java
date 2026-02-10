package org.example.homedatazip.tradeRent.repository;

import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRentRepository extends JpaRepository<TradeRent,Long> {
    Optional<TradeRent> findById(long id);
    List<TradeRent> findByApartmentId(long apartmentId);
    boolean existsByApartmentId(long apartmentId);

    List<TradeRent> findTop5ByApartment_IdOrderByDealDateDesc(Long aptId);

    @Query("""
        select tr from TradeRent tr
        where tr.apartment.id = :aptId
          and tr.exclusiveArea * 100 = :areaKey10
          and tr.dealDate >= :yyyymm
        order by tr.dealDate desc
    """)
    List<TradeRent> findByAptAndExclusiveKey10(
            @Param("aptId") long aptId,
            @Param("areaKey10") long areaKey10,
            @Param("yyyymm") LocalDate yyyymm
    );
}
