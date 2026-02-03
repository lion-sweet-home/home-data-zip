package org.example.homedatazip.listing.repository;

import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.type.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ListingStatus status);

    List<Listing> findByUserIdOrderByCreatedAtDesc(Long userId);

}
