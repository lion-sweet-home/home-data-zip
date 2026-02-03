package org.example.homedatazip.hospital.dto;

import org.example.homedatazip.hospital.entity.Hospital;

public record HospitalResponse(
        String name,
        String typeName,
        String address,
        Double latitude,
        Double longitude
) {

    public static HospitalResponse from(Hospital hospital) {
        return new HospitalResponse(
                hospital.getName(),
                hospital.getTypeName(),
                hospital.getAddress(),
                hospital.getLatitude(),
                hospital.getLongitude()
        );
    }
}
