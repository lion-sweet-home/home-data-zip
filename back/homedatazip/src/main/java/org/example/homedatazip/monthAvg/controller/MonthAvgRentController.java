package org.example.homedatazip.monthAvg.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.monthAvg.dto.*;
import org.example.homedatazip.monthAvg.service.MonthAvgRentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apartments/month-avg")
@RequiredArgsConstructor
public class MonthAvgRentController {

    private final MonthAvgRentService monthAvgRentService;

    // 아파트의 월별 거래량 첫 페이지
    @GetMapping("/{aptId}/total-rent")
    public ResponseEntity<List<MonthTotalTradeResponse>> monthRent(@PathVariable Long aptId,
                                                                   @RequestParam(name = "period", required = false) Integer periodRow) {

        List<MonthTotalTradeResponse> totalTrade = monthAvgRentService.getTotalTrade(aptId, periodRow);
        return ResponseEntity.ok(totalTrade);
    }

    //상세보기 카드에 area filter로 적용된 응답dto
    @GetMapping("/{aptId}/total-rent/area")
    public ResponseEntity<List<MonthTotalTradeAreaResponse>> monthRentArea(@PathVariable Long aptId,
                                                                       @RequestParam(name ="period",  required = false) Integer periodRow,
                                                                       @RequestParam Long areaKey ) {

        List<MonthTotalTradeAreaResponse> totalTradeWithExclusive = monthAvgRentService.getTotalTradeWithExclusive(aptId, periodRow, areaKey);
        return ResponseEntity.ok(totalTradeWithExclusive);
    }

    // 구까지만 조회 했을 때 나타나는 기간별 카운트
    @GetMapping("/jeonse-count")
    public ResponseEntity<List<JeonseCountResponse>>  JeonseCountByGu(@RequestParam String si,
                                                          @RequestParam String gu,
                                                          @RequestParam Integer period){
        List<JeonseCountResponse> countByGu = monthAvgRentService.getJeonseCountByGu(si, gu, period);

        return  ResponseEntity.ok(countByGu);
    }

    @GetMapping("/wolse-count")
    public ResponseEntity<List<WolseCountResponse>>  WolseCountByGu(@RequestParam String si,
                                                                      @RequestParam String gu,
                                                                      @RequestParam Integer period){
        List<WolseCountResponse> countByGu = monthAvgRentService.getWolseCountByGu(si, gu, period);

        return  ResponseEntity.ok(countByGu);
    }

    //아파트 detail page에서 평수 list 카드 데이터 호출
    @GetMapping("/{aptId}/detail/area-exclusive")
    public ResponseEntity<AreaTypeResponse> rentAreaExclusive(@PathVariable Long aptId) {

        AreaTypeResponse aptAreaKey = monthAvgRentService.getAptAreaKey(aptId);
        return ResponseEntity.ok(aptAreaKey);
    }

    //면적카드 선택시 나오는 최신 평균 거래가 (6개월)
    @GetMapping("/{aptId}/detail/area-exclusive/avg/recent")
    public ResponseEntity<AreaTypeMonthAvgResponse> monthsAvg(@PathVariable Long aptId,
                                                              @RequestParam Long areaKey){

        AreaTypeMonthAvgResponse monthsAvg = monthAvgRentService.getMonthsAvg(aptId, areaKey);
        return ResponseEntity.ok(monthsAvg);
    }

    //평수별, 월별 평균값 데이터 호출
    @GetMapping("/{aptId}/detail/area-excluesive/avg")
    public ResponseEntity<List<RentDetailAvg>> rentAreaTypeAvg(@PathVariable Long aptId,
                                                               @RequestParam Long areaKey,
                                                               @RequestParam(name = "period", required = false) Integer periodRow){
        List<RentDetailAvg> rentAreaTypeAvg = monthAvgRentService.getRentAreaTypeAvg(aptId, areaKey, periodRow);
        return ResponseEntity.ok(rentAreaTypeAvg);
    }

    //홈에 들어올 거래량 Top 3 등락률 전세
    @GetMapping("/jeonse")
    public ResponseEntity<List<MonthTop3JeonsePriceResponse>> jeonseTop3ByRegion(){
        List<MonthTop3JeonsePriceResponse> jeonseTop3ByRegion = monthAvgRentService.getJeonseTop3ByRegion();
        return ResponseEntity.ok(jeonseTop3ByRegion);
    }
    //홈에 들어올 거래량 Top 3 등락률 월세
    @GetMapping("/wolse")
    public ResponseEntity<List<MonthTop3WolsePriceResponse>> wolseTop3ByRegion(){
        List<MonthTop3WolsePriceResponse> wolseTop3ByRegion = monthAvgRentService.getWolseTop3ByRegion();
        return  ResponseEntity.ok(wolseTop3ByRegion);
    }
}
