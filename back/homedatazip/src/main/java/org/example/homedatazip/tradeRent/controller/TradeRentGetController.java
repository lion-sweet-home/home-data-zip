package org.example.homedatazip.tradeRent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.apartment.dto.MarkerClusterResponse;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.recommend.service.SearchLogService;
import org.example.homedatazip.recommend.type.LogType;
import org.example.homedatazip.recommend.type.TradeType;
import org.example.homedatazip.tradeRent.dto.DotResponse;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;
import org.example.homedatazip.tradeRent.dto.detailList.RentDetailList5Response;
import org.example.homedatazip.tradeRent.service.TradeRentGetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/rent")
@RequiredArgsConstructor
public class TradeRentGetController {

    private final TradeRentGetService tradeRentGetService;
    private final SearchLogService searchLogService;

    //좌표 마킹
    @GetMapping
    public ResponseEntity<List<MarkResponse>> markerRent(
            @ModelAttribute @Valid RentGetMarkerRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user != null && user.getUserId() != null) {
            searchLogService.saveRentSearchLog(user.getUserId(), request);
        }

        return ResponseEntity.ok(tradeRentGetService.getListRentsByAptId(request));
    }

    /**
     * bounds + level + (기존 필터) 기반 클러스터 조회
     * - 응답: [{ latitude, longitude, count }]
     */
    @GetMapping("/clusters")
    public ResponseEntity<List<MarkerClusterResponse>> markerRentClusters(
            @ModelAttribute @Valid RentGetMarkerRequest request
    ) {
        return ResponseEntity.ok(tradeRentGetService.getRentMarkerClusters(request));
    }

    //그래프에 점 데이터
    @GetMapping("/{aptId}/dots")
    public List<DotResponse> RentDot(@PathVariable Long aptId, @RequestParam Integer period){
        List<DotResponse> rentDot = tradeRentGetService.getRentDot(aptId, period);

        return ResponseEntity.ok(rentDot).getBody();
    }

    //마크 선택시 하단 최근 거래내역
    @GetMapping("/{aptId}")
    public ResponseEntity<List<RentFromAptResponse>> rentByAptId(@PathVariable Long aptId, @AuthenticationPrincipal CustomUserDetails user){

        if (user != null) {
            searchLogService.saveActionLog(user.getUserId(), aptId, LogType.SUMMARY, TradeType.RENT);
        }

        List<RentFromAptResponse> rentLimit5 = tradeRentGetService.getRentLimit5(aptId);
        return ResponseEntity.ok(rentLimit5);
    }

    //평형별 거래기록
    @GetMapping("/{aptId}/detail")
    public ResponseEntity<RentDetailList5Response> rentByAptDealDate(@PathVariable Long aptId,
                                                                     @RequestParam Long areaKey,
                                                                     @RequestParam(name = "period", required = false) Integer period,@AuthenticationPrincipal CustomUserDetails user){
        if (user != null) {
            searchLogService.saveActionLog(user.getUserId(), aptId, LogType.DETAIL, TradeType.RENT);
        }

        RentDetailList5Response rentAreaType = tradeRentGetService.getRentAreaType(aptId, areaKey, period);
        return ResponseEntity.ok(rentAreaType);
    }
}
