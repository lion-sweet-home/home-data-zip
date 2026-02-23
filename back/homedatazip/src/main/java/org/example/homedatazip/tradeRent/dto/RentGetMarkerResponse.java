package org.example.homedatazip.tradeRent.dto;

import org.example.homedatazip.apartment.entity.Apartment;

import java.util.List;

public record RentGetMarkerResponse (
        List<AptRentMarkerRequest> aptDtos

){
    public static RentGetMarkerResponse map(List<Apartment> apts){
        List<AptRentMarkerRequest> aptDtos = apts.stream()
                .map(AptRentMarkerRequest::from)
                .toList();
        return new RentGetMarkerResponse(aptDtos);
    }

    public record AptRentMarkerRequest(
            Long aptId,
            String aptName,

            Double longitude,
            Double latitude
    ){
        public static AptRentMarkerRequest from (Apartment apt){
            return new AptRentMarkerRequest(
                    apt.getId(),
                    apt.getAptName(),
                    apt.getLongitude(),
                    apt.getLatitude()
            );
        }
    }
}
