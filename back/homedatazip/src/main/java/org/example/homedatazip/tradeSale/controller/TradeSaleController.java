package org.example.homedatazip.tradeSale.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.tradeSale.dto.AptChartResponse;
import org.example.homedatazip.tradeSale.dto.AptDetailResponse;
import org.example.homedatazip.tradeSale.dto.AptSaleSummaryResponse;
import org.example.homedatazip.tradeSale.dto.SaleSearchRequest;
import org.example.homedatazip.tradeSale.service.TradeSaleQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/apartment/trade-sale")
@RequiredArgsConstructor
public class TradeSaleController {

    private final TradeSaleQueryService tradeSaleQueryService;

    @GetMapping("/markers")
    public ResponseEntity<List<MarkResponse>> getMarkers(@Valid SaleSearchRequest req) {
        log.info("[SALE_SEARCH] sido='{}', gugun='{}', dong='{}', keyword='{}', minAmount={}, maxAmount={}, periodMonths={}",
                req.sido(), req.gugun(), req.dong(), req.keyword(),
                req.minAmount(), req.maxAmount(), req.periodMonths());
        return ResponseEntity.ok(tradeSaleQueryService.getMarkers(req));
    }

    // 아파트 요약
    @GetMapping("/{aptId}/summary")
    public ResponseEntity<AptSaleSummaryResponse> getAptSummary(
            @PathVariable Long aptId,
            @RequestParam(name = "periodMonths", required = false, defaultValue = "6") Integer periodMonths
    ) {
        AptSaleSummaryResponse response = tradeSaleQueryService.getAptSaleSummary(aptId, periodMonths);

        return ResponseEntity.ok(response);
    }

    // 아파트 상세 보기
    @GetMapping("/{aptId}/detail")
    public ResponseEntity<AptDetailResponse> getAptDetail(
            @PathVariable Long aptId,
            @RequestParam(name = "periodMonths", defaultValue = "6") Integer periodMonths
    ) {
        AptDetailResponse response = tradeSaleQueryService.getAptDetail(aptId, periodMonths);
        return ResponseEntity.ok(response);
    }

    // 차트 데이터만 별도 조회
    @GetMapping("/{aptId}/chart")
    public ResponseEntity<AptChartResponse> getAptChart(
            @PathVariable Long aptId,
            @RequestParam(name = "periodMonths", defaultValue = "6") Integer periodMonths
    ) {
        AptChartResponse response = tradeSaleQueryService.getAptChartOnly(aptId, periodMonths);
        return ResponseEntity.ok(response);
    }
}
