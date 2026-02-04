package org.example.homedatazip.tradeRent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.dto.MarkResponse;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerResponse;
import org.example.homedatazip.tradeRent.service.TradeRentGetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/rent")
@RequiredArgsConstructor
public class TradeRentGetController {

    private final TradeRentGetService tradeRentGetService;

    //좌표 마킹
    @GetMapping
    public ResponseEntity<List<MarkResponse>> getMarkerRent(
            @ModelAttribute @Valid RentGetMarkerRequest request
    ) {
        return ResponseEntity.ok(tradeRentGetService.getListRentsByAptId(request));
    }

    //마크 선택시 하단 최근 거래내역
    @GetMapping("/{aptId}")
    public ResponseEntity<List<RentFromAptResponse>> getRentByAptId(@PathVariable @Valid Long aptId){
        List<RentFromAptResponse> rentLimit5 = tradeRentGetService.getRentLimit5(aptId);
        return ResponseEntity.ok(rentLimit5);
    }
}
