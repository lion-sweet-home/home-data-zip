package org.example.homedatazip.listing.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.listing.dto.ListingCreateRequest;
import org.example.homedatazip.listing.dto.ListingDetailResponse;
import org.example.homedatazip.listing.dto.ListingSearchResponse;
import org.example.homedatazip.listing.dto.MyListingResponse;
import org.example.homedatazip.listing.service.ListingCommandService;
import org.example.homedatazip.listing.service.ListingService;
import org.example.homedatazip.listing.service.ListingQueryService;
import org.example.homedatazip.listing.type.RentType;
import org.example.homedatazip.listing.type.TradeType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;
    private final ListingCommandService listingCommandService;
    private final ListingQueryService listingQueryService;

    // 매물 등록
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ListingCreateRequest request
    ) {
        Long listingId = listingCommandService.create(principal.getUserId(), request);

        return ResponseEntity.ok(Map.of(
                "listingId", listingId
        ));
    }

    // 내 매물 조회
    @GetMapping("/me")
    public ResponseEntity<List<MyListingResponse>> myListings(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(
                listingQueryService.myListings(principal.getUserId(), status)
        );
    }

    // 전체 매물 조회
    @GetMapping
    public ResponseEntity<List<ListingSearchResponse>> search(
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String gugun,
            @RequestParam(required = false) String dong,
            @RequestParam(required = false) String apartmentName,
            @RequestParam(required = false) TradeType tradeType,
            @RequestParam(required = false) RentType rentType,
            @RequestParam(defaultValue = "50") int limit
    ) {
        if (tradeType == null && rentType != null) {
            tradeType = TradeType.RENT;
        }

        return ResponseEntity.ok(
                listingQueryService.search(sido, gugun, dong, apartmentName, tradeType, rentType, limit)
        );
    }

    // 매매만
    @GetMapping("/sale")
    public ResponseEntity<List<ListingSearchResponse>> sale(
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String gugun,
            @RequestParam(required = false) String dong,
            @RequestParam(required = false) String apartmentName,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(
                listingQueryService.search(sido, gugun, dong, apartmentName, TradeType.SALE, null, limit)
        );
    }

    // 전/월세만
    @GetMapping("/rent")
    public ResponseEntity<List<ListingSearchResponse>> rent(
            @RequestParam(required = false) String sido,
            @RequestParam(required = false) String gugun,
            @RequestParam(required = false) String dong,
            @RequestParam(required = false) String apartmentName,
            @RequestParam(required = false) RentType rentType,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(
                listingQueryService.search(sido, gugun, dong, apartmentName, TradeType.RENT, rentType, limit)
        );
    }


    // 상세 조회 (이미지 포함)
    @GetMapping("/{listingId}")
    public ResponseEntity<ListingDetailResponse> detail(@PathVariable Long listingId) {
        return ResponseEntity.ok(listingQueryService.detail(listingId));
    }

    // 삭제(soft delete) - 본인 매물만
    @DeleteMapping("/{listingId}")
    public ResponseEntity<Map<String, Object>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long listingId
    ) {
        listingService.delete(principal.getUserId(), listingId);
        return ResponseEntity.ok(Map.of(
                "listingId", listingId,
                "deleted", true
        ));
    }
}
