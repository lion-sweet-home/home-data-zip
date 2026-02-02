package org.example.homedatazip.global.geo;

public final class GeoBox {

    private GeoBox() {}

    public static BoundingBox boundingBox(double lat, double lon, double radiusMeters) {
        double latRad = Math.toRadians(lat);

        double deltaLat = radiusMeters / 111_000.0;
        double deltaLon = radiusMeters / (111_000.0 * Math.cos(latRad));

        return new BoundingBox(
                lat - deltaLat,
                lat + deltaLat,
                lon - deltaLon,
                lon + deltaLon
        );
    }

    public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {}
}
