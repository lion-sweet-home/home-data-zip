package org.example.homedatazip.busstation.dto;

public record NearbyBusStationReponse(
        Long id,
        String nodeId,
        String stationNumber,
        String name,
        double longitude,
        double latitude,
        double distanceMeters
) {}
