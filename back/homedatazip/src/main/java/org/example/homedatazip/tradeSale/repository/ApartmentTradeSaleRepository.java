package org.example.homedatazip.tradeSale.repository;

import org.example.homedatazip.tradeSale.entity.TradeSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ApartmentTradeSaleRepository extends JpaRepository<TradeSale, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT IGNORE INTO trade_sale 
            (apartment_id, deal_amount, exclusive_area, floor, apt_dong, deal_date, sgg_cd, canceled) 
            VALUES (:apartmentId, :amount, :area, :floor, :dong, :dealDate, :sggCd, :canceled)
            """, nativeQuery = true)
    void insertIgnore(@Param("apartmentId") Long apartmentId,
                      @Param("amount") Long amount,
                      @Param("area") Double area,
                      @Param("floor") Integer floor,
                      @Param("dong") String dong,
                      @Param("dealDate") java.time.LocalDate dealDate,
                      @Param("sggCd") String sggCd,
                      @Param("canceled") Boolean canceled);
}
