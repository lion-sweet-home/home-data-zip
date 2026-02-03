package org.example.homedatazip.hospital.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.hospital.dto.HospitalResponse;
import org.example.homedatazip.hospital.dto.HospitalStatsResponse;
import org.example.homedatazip.hospital.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    /**
     * 병원 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getHospitalCount(
            @RequestParam String sido,
            @RequestParam String gugun,
            @RequestParam String dong
    ) {
        return ResponseEntity.ok()
                .body(hospitalService.getHospitalCount(sido, gugun, dong));
    }

    /**
     * 병원 종류별 개수 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<HospitalStatsResponse> getHospitalStats(
            @RequestParam String sido,
            @RequestParam String gugun,
            @RequestParam String dong
    ) {
        return ResponseEntity.ok()
                .body(hospitalService.getHospitalStats(sido, gugun, dong));
    }

    /**
     * 병원 목록 조회 (마커용)
     */
    @GetMapping("/list")
    public ResponseEntity<List<HospitalResponse>> getHospitalList(
            @RequestParam String sido,
            @RequestParam String gugun,
            @RequestParam String dong
    ) {
        return ResponseEntity.ok()
                .body(hospitalService.getHospitalList(sido, gugun, dong));
    }
}
