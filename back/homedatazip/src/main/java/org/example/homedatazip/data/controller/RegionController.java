package org.example.homedatazip.data.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.data.repository.RegionRepository;
import org.example.homedatazip.data.service.RegionService;
import org.example.homedatazip.tradeSale.dto.DongRankResponse;
import org.example.homedatazip.tradeSale.service.TradeSaleQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
//dong-options  정렬을 위해 사용
import java.util.Comparator;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;
    private final RegionRepository regionRepository;
    private final TradeSaleQueryService tradeSaleQueryService;

    @GetMapping("/sido")
    public ResponseEntity<List<String>> getSidoList() {

        List<String> list = regionService.findSidoList().stream()
                .filter(s -> s != null && !s.isBlank())    // null/빈값 제거
                .distinct()                 // 혹시 중복 있으면 제거
                .sorted()                   // 드롭다운 정렬
                .toList();

        return ResponseEntity.ok(regionService.findSidoList());
    }

    @GetMapping("/gugun")
    public ResponseEntity<List<String>> getGugunList(@RequestParam String sido) {
        require(sido, "sido");
        List<String> list = regionService.findGugunList(sido).stream()
                .filter(this::notBlank)
                .distinct()
                .sorted()

                .toList();
        return ResponseEntity.ok(regionService.findGugunList(sido));
    }

    @GetMapping("/dong")
    public ResponseEntity<List<String>> getDongList(@RequestParam String sido, @RequestParam String gugun) {
        require(sido, "sido");
        require(gugun, "gugun");

        List<String> list = regionService.findDongList(sido, gugun).stream()
                .filter(this::notBlank)
                .distinct()
                .sorted()
                .toList();

        return ResponseEntity.ok(regionService.findDongList(sido, gugun));
    }

    @GetMapping("/dong/rank")
    public ResponseEntity<List<DongRankResponse>> getDongRank(
            @RequestParam String sido,
            @RequestParam String gugun,
            @RequestParam(defaultValue = "6") int periodMonths) {

        require(sido, "sido");
        require(gugun, "gugun");

        return ResponseEntity.ok(tradeSaleQueryService.getDongRanking(sido, gugun, periodMonths));
    }

    //regionID 프론트에서 바로 확보
    @GetMapping("/dong-options")
    public ResponseEntity<List<DongOptionResponse>> getDongOptions(
            @RequestParam String sido,
            @RequestParam String gugun
    ) {
        require(sido, "sido");
        require(gugun, "gugun");

        List<DongOptionResponse> result = regionRepository.findRegionsBySidoAndGugun(sido, gugun).stream()
                .filter(r -> notBlank(r.getDong())) // dong 빈값 제거
                .map(r -> new DongOptionResponse(r.getId(), r.getDong()))
                .sorted(Comparator.comparing(DongOptionResponse::dong)) //  dong 기준 정렬
                .toList();

        return ResponseEntity.ok(result);
    }

    private void require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }


    public record DongOptionResponse(Long regionId, String dong) {}
}
