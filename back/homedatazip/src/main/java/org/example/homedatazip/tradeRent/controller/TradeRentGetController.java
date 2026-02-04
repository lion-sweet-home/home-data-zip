package org.example.homedatazip.tradeRent.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.tradeRent.dto.RentFromAptResponse;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerRequest;
import org.example.homedatazip.tradeRent.dto.RentGetMarkerResponse;
import org.example.homedatazip.tradeRent.service.TradeRentGetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/rent")
@RequiredArgsConstructor
public class TradeRentGetController {

    private final TradeRentGetService tradeRentGetService;

    @GetMapping
    public ResponseEntity<RentGetMarkerResponse> getMarkerRent(
            @ModelAttribute @Valid RentGetMarkerRequest request
    ) {
        return ResponseEntity.ok(tradeRentGetService.getListRentsByAptId(request));
    }

    @GetMapping("/{apt-id}")
    public ResponseEntity<RentFromAptResponse> getRentByAptId(@PathVariable @Valid Long aptId){
        RentFromAptResponse rentLimit5 = tradeRentGetService.getRentLimit5(aptId);
        return ResponseEntity.ok(rentLimit5);
    }
}
