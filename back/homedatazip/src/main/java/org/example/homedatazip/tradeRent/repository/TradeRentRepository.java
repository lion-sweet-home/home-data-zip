package org.example.homedatazip.tradeRent.repository;

import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRentRepository extends JpaRepository<TradeRent,Long> {
}
