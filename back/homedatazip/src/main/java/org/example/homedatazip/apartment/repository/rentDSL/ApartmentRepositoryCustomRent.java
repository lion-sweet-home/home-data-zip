package org.example.homedatazip.apartment.repository.rentDSL;

import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;

import java.util.List;

public interface ApartmentRepositoryCustomRent {
    List<Apartment> findAllWithRentByRegionAndRentRange(RentGetMarkerRequest request) ;
}
