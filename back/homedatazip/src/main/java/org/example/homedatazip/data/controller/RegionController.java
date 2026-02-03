package org.example.homedatazip.data.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.data.repository.RegionRepository;
import org.example.homedatazip.data.service.RegionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;
    private final RegionRepository regionRepository;

    @GetMapping("/sido")
    public ResponseEntity<List<String>> getSidoList() {
        return ResponseEntity.ok(regionService.findSidoList());
    }

    @GetMapping("/gugun")
    public ResponseEntity<List<String>> getGugunList(@RequestParam String sido) {
        return ResponseEntity.ok(regionService.findGugunList(sido));
    }

    @GetMapping("/dong")
    public ResponseEntity<List<String>> getDongList(@RequestParam String sido, @RequestParam String gugun) {

        return ResponseEntity.ok(regionService.findDongList(sido, gugun));
    }

    //regionID 프론트에서 바로 확보
    @GetMapping("/dong-options")
    public ResponseEntity<List<DongOptionResponse>> getDongOptions(
            @RequestParam String sido,
            @RequestParam String gugun
    ) {
        List<DongOptionResponse> result = regionRepository.findRegionsBySidoAndGugun(sido, gugun).stream()
                .map(r -> new DongOptionResponse(r.getId(), r.getDong()))
                .toList();

        return ResponseEntity.ok(result);
    }

    public record DongOptionResponse(Long regionId, String dong) {}
}
