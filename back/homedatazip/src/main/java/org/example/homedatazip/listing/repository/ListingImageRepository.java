package org.example.homedatazip.listing.repository;

import org.example.homedatazip.listing.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {

    List<ListingImage> findByListingIdOrderByMainDescSortOrderAscIdAsc(Long listingId);

    List<ListingImage> findByListingId(Long listingId);

    void deleteByListingId(Long listingId);
}
