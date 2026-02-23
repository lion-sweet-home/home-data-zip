package org.example.homedatazip.apartment.repository.rentDSL;

import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.apartment.dto.MarkerClusterResponse;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;

import java.util.List;

public interface ApartmentRepositoryCustomRent {
    List<MarkResponse> findAllWithRentByRegionAndRentRange(RentGetMarkerRequest request) ;

    /**
     * bounds + (기존 필터) 기준으로 마커를 격자(grid) 단위로 묶어서 집계(count)한 클러스터 목록을 반환한다.
     * 줌 레벨(level)에 따라 grid 크기를 다르게 적용한다.
     */
    List<MarkerClusterResponse> findRentMarkerClusters(RentGetMarkerRequest request);
}
