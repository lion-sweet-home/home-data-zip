package org.example.homedatazip.monthAvg.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.monthAvg.dto.AreaTypeResponse;
import org.example.homedatazip.monthAvg.dto.MonthTotalTradeAreaResponse;
import org.example.homedatazip.monthAvg.dto.MonthTotalTradeResponse;
import org.example.homedatazip.monthAvg.service.MonthAvgRentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apartments/{aptId}")
@RequiredArgsConstructor
public class MonthAvgRentController {

    private final MonthAvgRentService monthAvgRentService;

    // 아파트의 월별 거래량 첫 페이지
    @GetMapping("/total-rent")
    public ResponseEntity<List<MonthTotalTradeResponse>> monthRent(@PathVariable Long aptId,
                                                                   @RequestParam(name = "period", required = false) String periodRow) {

        List<MonthTotalTradeResponse> totalTrade = monthAvgRentService.getTotalTrade(aptId, periodRow);
        return ResponseEntity.ok(totalTrade);
    }

    //상세보기 카드에 area filter로 적용된 응답dto
    @GetMapping("/total-rent/area")
    public ResponseEntity<List<MonthTotalTradeAreaResponse>> monthRentArea(@PathVariable Long aptId,
                                                                       @RequestParam(name ="period",  required = false) String periodRow,
                                                                       @RequestParam Long areaKey ) {

        List<MonthTotalTradeAreaResponse> totalTradeWithExclusive = monthAvgRentService.getTotalTradeWithExclusive(aptId, periodRow, areaKey);
        return ResponseEntity.ok(totalTradeWithExclusive);
    }

    //아파트 detail page에서 평수 list 카드 데이터 호출
    @GetMapping("/detail/area-exclusive")
    public ResponseEntity<AreaTypeResponse> rentAreaExclusive(@PathVariable Long aptId) {

        AreaTypeResponse aptAreaKey = monthAvgRentService.getAptAreaKey(aptId);
        return ResponseEntity.ok(aptAreaKey);
    }
}
