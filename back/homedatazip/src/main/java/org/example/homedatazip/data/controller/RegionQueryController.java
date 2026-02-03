package org.example.homedatazip.region.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.data.repository.RegionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/region-options")
public class RegionQueryController {

    private final RegionRepository regionRepository;

    @GetMapping("/sidos")
    public ResponseEntity<List<String>> sidos() {
        List<String> result = regionRepository.findDistinctSido().stream()
                .filter(s -> s != null && !s.isBlank())
                .sorted()
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/guguns")
    public ResponseEntity<List<String>> guguns(@RequestParam String sido) {
        List<String> result = regionRepository.findDistinctGugunBySido(sido).stream()
                .filter(g -> g != null && !g.isBlank())
                .sorted()
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/dongs")
    public ResponseEntity<List<DongOptionResponse>> dongs(
            @RequestParam String sido,
            @RequestParam String gugun
    ) {
        List<DongOptionResponse> result = regionRepository.findBySidoAndGugun(sido, gugun).stream()
                .filter(r -> r.getDong() != null && !r.getDong().isBlank())
                .sorted(Comparator.comparing(Region::getDong))
                .map(r -> new DongOptionResponse(r.getId(), r.getDong()))
                .toList();

        return ResponseEntity.ok(result);
    }

    public record DongOptionResponse(Long regionId, String dong) {}
}
