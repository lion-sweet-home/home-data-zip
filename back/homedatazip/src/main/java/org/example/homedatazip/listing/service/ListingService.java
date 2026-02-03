package org.example.homedatazip.listing.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.ListingErrorCode;
import org.example.homedatazip.listing.dto.ListingCreateRequest;
import org.example.homedatazip.listing.dto.MyListingResponse;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.listing.type.ListingStatus;
import org.example.homedatazip.listing.type.TradeType;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;


}
