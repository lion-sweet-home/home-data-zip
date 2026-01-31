package org.example.homedatazip.hospital.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.hospital.dto.HospitalStatsResponse;
import org.example.homedatazip.hospital.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping("/count")
    public ResponseEntity<Long> getHospitalCountByDong(
            @RequestParam String dong
    ) {
        return ResponseEntity.ok()
                .body(hospitalService.getHospitalCountByDong(dong));
    }

    @GetMapping("/stats")
    public ResponseEntity<HospitalStatsResponse> getHospitalStatsByDong(
            @RequestParam String dong
    ) {
        return ResponseEntity.ok()
                .body(hospitalService.getHospitalStatsByDong(dong));
    }
}
