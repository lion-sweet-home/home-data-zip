package org.example.homedatazip.recommend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.recommend.dto.ApartmentResponse;
import org.example.homedatazip.recommend.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<ApartmentResponse>> getMyRecommendations(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        if (customUserDetails == null) {
            log.info("[Recommend] Guest user — no recommendations (login required for personalized data)");
            return ResponseEntity.ok(List.of());
        }

        Long userId = customUserDetails.getUserId();
        log.info("[Recommend] User {} requested personalized recommendations", userId);

        List<ApartmentResponse> recommendations = recommendationService.getPersonalizedRecommendations(userId);

        return ResponseEntity.ok(recommendations);
    }
}
