package org.example.homedatazip.global.util;

/** 위·경도 두 점 사이의 대권 거리(km)를 하버사인 공식으로 계산 */
public final class HaversineUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineUtils() {
    }

    /**
     * 두 위·경도 점 사이의 거리(km).
     *
     * @param lat1 위도1 (도 단위)
     * @param lon1 경도1 (도 단위)
     * @param lat2 위도2 (도 단위)
     * @param lon2 경도2 (도 단위)
     * @return 거리(km), 같은 점이면 0
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
