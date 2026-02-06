package org.example.homedatazip.school.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.school.dto.ApartmentNearSchoolResponse;
import org.example.homedatazip.school.dto.SchoolResponse;
import org.example.homedatazip.school.dto.SchoolSearchRequest;
import org.example.homedatazip.school.service.SchoolService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schools")
public class SchoolController {

    private final SchoolService schoolService;

    /** 시도·구군(필수), 동(옵션)으로 학교 목록 검색 */
    @GetMapping()
    public ResponseEntity<List<SchoolResponse>> searchSchoolsByRegion(
            @ModelAttribute SchoolSearchRequest request
    ) {
        List<SchoolResponse> list = schoolService.searchSchoolsByRegion(request);
        return ResponseEntity.ok(list);
    }

    /** 해당 학교 반경 내 아파트 검색 */
    @GetMapping("/{schoolId}/apartments")
    public ResponseEntity<List<ApartmentNearSchoolResponse>> getApartmentsNearSchool(
            @PathVariable Long schoolId,
            @RequestParam double distanceKm
    ) {
        List<ApartmentNearSchoolResponse> apartments = schoolService.findApartmentsNearSchool(schoolId, distanceKm);
        return ResponseEntity.ok(apartments);
    }
}