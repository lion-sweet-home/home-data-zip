package org.example.homedatazip.listing.repository;

import org.example.homedatazip.listing.entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {
}
