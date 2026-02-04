package org.example.homedatazip.tradeRent.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerResponse;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.example.homedatazip.tradeRent.repository.TradeRentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeRentGetService {

    private final TradeRentRepository tradeRentRepository;
    private final ApartmentRepository apartmentRepository;

    //마커용 렌트 조회 지도에 뿌려주는 데이터
    @Transactional(readOnly = true)
    public RentGetMarkerResponse getListRentsByAptId(RentGetMarkerRequest dto){

        System.out.println("[marker] request dto = " + dto);

        List<Apartment> apts = apartmentRepository.findAllWithRentByRegionAndRentRange(
                dto.sido(), dto.gugun() , dto.dong(),
                dto.minDeposit(), dto.maxDeposit(),
                dto.minMonthlyRent(),  dto.maxMonthlyRent());

        System.out.println("[marker] result size = " + apts.size());

        return RentGetMarkerResponse.map(apts);
    }

    //TradeRent 최근 거래내역 5개 / 지도 -> 마커 선택 시 생기는 창
    @Transactional(readOnly = true)
    public RentFromAptResponse getRentLimit5(long aptId){
        List<TradeRent> rentTop5 = tradeRentRepository.findTop5ByApartment_IdOrderByDealDateDesc(aptId);

        return RentFromAptResponse.map(rentTop5);
    }

    //Detail 조회용

    //Detail 평형기준 월별 보증금, 월세 평균

    //Detail


}
