package org.example.homedatazip.tradeRent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.apartment.dto.MarkerClusterResponse;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.tradeRent.dto.DotResponse;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.dto.detailList.RentDetailList5Response;
import org.example.homedatazip.tradeRent.entity.TradeRent;
import org.example.homedatazip.tradeRent.repository.TradeRentDSLRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeRentGetService {

    private final TradeRentDSLRepository tradeRentDSLRepository;
    private final ApartmentRepository apartmentRepository;


    //마커용 렌트 조회 지도에 뿌려주는 데이터
    @Transactional(readOnly = true)
    public List<MarkResponse> getListRentsByAptId(RentGetMarkerRequest dto){

        return apartmentRepository.findAllWithRentByRegionAndRentRange(dto);
    }

    /**
     * 줌 레벨(level) + bounds + (기존 필터) 기반으로 격자 클러스터를 반환한다.
     */
    @Transactional(readOnly = true)
    public List<MarkerClusterResponse> getRentMarkerClusters(RentGetMarkerRequest dto) {
        return apartmentRepository.findRentMarkerClusters(dto);
    }


    //TradeRent 최근 거래내역  지도
    @Transactional(readOnly = true)
    public List<RentFromAptResponse> getRentLimit5(long aptId){
        return tradeRentDSLRepository.findRecent5(aptId);

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
}
