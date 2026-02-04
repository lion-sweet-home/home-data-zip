package org.example.homedatazip.tradeRent.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.monthAvg.entity.MonthAvg;
import org.example.homedatazip.monthAvg.repository.MonthAvgRepository;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerResponse;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.dto.detailList.RentAreaType;
import org.example.homedatazip.tradeRent.dto.detailList.RentDetailList5Response;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.example.homedatazip.tradeRent.repository.TradeRentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeRentGetService {

    private final TradeRentRepository tradeRentRepository;
    private final MonthAvgRepository monthAvgRepository;
    private final ApartmentRepository apartmentRepository;

    //마커용 렌트 조회 지도에 뿌려주는 데이터
    @Transactional(readOnly = true)
    public List<MarkResponse> getListRentsByAptId(RentGetMarkerRequest dto){

        System.out.println("[marker] request dto = " + dto);

        List<Apartment> apts = apartmentRepository.findAllWithRentByRegionAndRentRange(dto);

        List<MarkResponse> list = apts.stream()
                .map(MarkResponse::from)
                .toList();

        System.out.println("[marker] result size = " + apts.size());

        return list;
    }

    //TradeRent 최근 거래내역 5개 / 지도 -> 마커 선택 시 생기는 창
    @Transactional(readOnly = true)
    public List<RentFromAptResponse> getRentLimit5(long aptId){
        List<TradeRent> rentTop5 = tradeRentRepository.
                findTop5ByApartment_IdOrderByDealDateDesc(aptId,PageRequest.of(0, 5));

        List<RentFromAptResponse> list = rentTop5.stream()
                .map(RentFromAptResponse::from)
                .toList();

        return list;
    }

    //Detail 조회용 평형기준 최근 list 5개씩 보증금, 월세 조회
    @Transactional(readOnly = true)
    public RentDetailList5Response getRentAreaType(long aptId, long areaKey10){

        List<TradeRent> top5 = tradeRentRepository
                .findByAptAndExclusiveKey10(aptId, areaKey10, PageRequest.of(0,5));

        return RentDetailList5Response.from(top5,aptId,areaKey10);
    }

    //Detail 평형기준 월별 보증금, 월세 평균

    //Detail


    private int key10(double exclusive){
        return (int) Math.round(exclusive * 10);
    }
    private double toExclusive(Long areaType){
        return (areaType % 1_000_000) / 10.0;
    }
    private Long getAreaType(Long aptId, double exclusive){
        return (aptId * 1_000_000) + key10(exclusive);
    }
}
