package org.example.homedatazip.tradeRent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.tradeRent.dto.DotResponse;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.dto.detailList.RentDetailList5Response;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.example.homedatazip.tradeRent.repository.TradeRentDSLRepository;
import org.example.homedatazip.tradeRent.repository.TradeRentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeRentGetService {

    private final TradeRentRepository tradeRentRepository;
    private final TradeRentDSLRepository tradeRentDSLRepository;
    private final ApartmentRepository apartmentRepository;
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");


    //마커용 렌트 조회 지도에 뿌려주는 데이터
    @Transactional(readOnly = true)
    public List<MarkResponse> getListRentsByAptId(RentGetMarkerRequest dto){

        List<Apartment> apts = apartmentRepository.findAllWithRentByRegionAndRentRange(dto);

        List<MarkResponse> list = apts.stream()
                .map(MarkResponse::from)
                .toList();


        return list;
    }


    //TradeRent 최근 거래내역  지도
    @Transactional(readOnly = true)
    public List<RentFromAptResponse> getRentLimit5(long aptId){
        List<TradeRent> rentTop5 = tradeRentDSLRepository.findRecent5(aptId);

        List<RentFromAptResponse> list = rentTop5.stream()
                .map(RentFromAptResponse::from)
                .toList();

        return list;
    }

    //그래프에 점 데이터
    @Transactional(readOnly = true)
    public List<DotResponse> getRentDot(long aptId, int period){
        period = period == 0 ? 6 : period;
        long periodLong = period;
        YearMonth startYm = YearMonth.now().minusMonths(periodLong - 1L);
        LocalDate startDate = startYm.atDay(1);

        List<DotResponse> dot = tradeRentDSLRepository.findDot(aptId, startDate);
        log.info("Map Size {}", dot.size());
        return dot;
    }

    //Detail 조회용 평형기준 최근 보증금, 월세 조회
    @Transactional(readOnly = true)
    public RentDetailList5Response getRentAreaType(long aptId, long areaKey10, int period){
        period = period == 0 ? 6 : period;
        long periodLong = period;
        YearMonth startYm = YearMonth.now().minusMonths(periodLong - 1L);
        LocalDate startDate = startYm.atDay(1);

        List<TradeRent> top = tradeRentDSLRepository
                .findItemsByArea(aptId, areaKey10, startDate);
        log.info("Map Size {}", top.size());

        return RentDetailList5Response.from(top,aptId,areaKey10);
    }




    private int key10(double exclusive){
        return (int) Math.round(exclusive * 100);
    }
    private double toExclusive(Long areaType){
        return (areaType % 1_000_000) / 100.0;
    }
    private Long getAreaType(Long aptId, double exclusive){
        return (aptId * 1_000_000) + key10(exclusive);
    }

    public static LocalDate parseYyyymmToMonthStart(int yyyymm) {

        String s = String.format("%06d", yyyymm); // 202511
        int yyyy = Integer.parseInt(s.substring(0, 4));
        int mm = Integer.parseInt(s.substring(4, 6));
        return YearMonth.of(yyyy, mm).atDay(1);
    }
}
