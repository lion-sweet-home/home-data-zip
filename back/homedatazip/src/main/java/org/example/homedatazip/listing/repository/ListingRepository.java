package org.example.homedatazip.listing.repository;

import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.type.ListingStatus;
import org.example.homedatazip.listing.type.TradeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ListingStatus status);

    List<Listing> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
        SELECT l
        FROM Listing l
        WHERE l.status = 'ACTIVE'
          AND (:regionId IS NULL OR l.region.id = :regionId)
          AND (:apartmentId IS NULL OR l.apartment.id = :apartmentId)
          AND (:tradeType IS NULL OR l.tradeType = :tradeType)
        ORDER BY l.createdAt DESC
    """)
    List<Listing> searchActive(Long regionId, Long apartmentId, TradeType tradeType, Pageable pageable);

    default List<Listing> searchActive(Long regionId, Long apartmentId, TradeType tradeType, int limit) {
        return searchActive(regionId, apartmentId, tradeType, Pageable.ofSize(limit));
    }

}
