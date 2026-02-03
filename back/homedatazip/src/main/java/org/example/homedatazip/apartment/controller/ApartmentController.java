package org.example.homedatazip.apartment.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.data.dto.ApartmentOptionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apartments")
public class ApartmentController {

    private final ApartmentRepository apartmentRepository;

    @GetMapping
    public ResponseEntity<List<ApartmentOptionResponse>> apartments(@RequestParam Long regionId) {
        List<Apartment> apartments = apartmentRepository.findByRegionIdOrderByAptNameAsc(regionId);

        List<ApartmentOptionResponse> result = apartments.stream()
                .map(a -> new ApartmentOptionResponse(a.getId(), a.getAptName()))
                .toList();

        return ResponseEntity.ok(result);
    }
}
