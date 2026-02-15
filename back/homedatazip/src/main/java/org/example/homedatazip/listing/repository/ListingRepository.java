package org.example.homedatazip.listing.repository;

import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.type.ListingStatus;
import org.example.homedatazip.listing.type.TradeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ListingStatus status);

    List<Listing> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
        select distinct l
        from Listing l
        join fetch l.region r
        join fetch l.apartment a
        left join fetch l.images i
        where l.status = org.example.homedatazip.listing.type.ListingStatus.ACTIVE
          and (:regionId is null or r.id = :regionId)
          and (:apartmentId is null or a.id = :apartmentId)
          and (:tradeType is null or l.tradeType = :tradeType)
        order by l.createdAt desc
    """)
    List<Listing> searchActive(Long regionId, Long apartmentId, TradeType tradeType, Pageable pageable);

    default List<Listing> searchActive(Long regionId, Long apartmentId, TradeType tradeType, int limit) {
        return searchActive(regionId, apartmentId, tradeType, Pageable.ofSize(limit));
    }

    // 상세조회도 ACTIVE만 (삭제된 매물은 조회 불가)
    @Query("""
        select distinct l
        from Listing l
        left join fetch l.images i
        left join fetch l.apartment a
        left join fetch l.region r
        where l.id = :listingId
          and l.status = org.example.homedatazip.listing.type.ListingStatus.ACTIVE
    """)
    Optional<Listing> findDetailWithImages(@Param("listingId") Long listingId);
}
