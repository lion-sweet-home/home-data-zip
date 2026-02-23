package org.example.homedatazip.listing.repository;

import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.type.ListingStatus;
import org.example.homedatazip.listing.type.TradeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
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
          and (:sido is null or :sido = '' or r.sido = :sido)
          and (:gugun is null or :gugun = '' or r.gugun = :gugun)
          and (:dong is null or :dong = '' or r.dong = :dong)
          and (:apartmentName is null or :apartmentName = '' 
               or lower(a.aptName) like lower(concat('%', :apartmentName, '%')))
          and (:tradeType is null or l.tradeType = :tradeType)
        order by l.createdAt desc
    """)
    List<Listing> searchActiveByFilters(
            @Param("sido") String sido,
            @Param("gugun") String gugun,
            @Param("dong") String dong,
            @Param("apartmentName") String apartmentName,
            @Param("tradeType") TradeType tradeType,
            Pageable pageable
    );

    // limit 지원용 default 메서드
    default List<Listing> searchActiveByFilters(
            String sido,
            String gugun,
            String dong,
            String apartmentName,
            TradeType tradeType,
            int limit
    ) {
        return searchActiveByFilters(
                sido,
                gugun,
                dong,
                apartmentName,
                tradeType,
                PageRequest.of(0, limit)
        );
    }

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
