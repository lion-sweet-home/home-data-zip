package org.example.homedatazip.tradeRent.repository;

import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRentRepository extends JpaRepository<TradeRent,Long> {
    Optional<TradeRent> findById(long id);
    List<TradeRent> findByApartmentId(long apartmentId);
    boolean existsByApartmentId(long apartmentId);

    List<TradeRent> findTop5ByApartment_IdOrderByDealDateDesc(Long aptId);
}
