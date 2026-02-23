package org.example.homedatazip.recommend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.recommend.dto.ApartmentResponse;
import org.example.homedatazip.recommend.repository.ApartmentRecommendationRepository;
import org.example.homedatazip.recommend.repository.UserSearchLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final UserSearchLogRepository logRepository;
    private final ApartmentRecommendationRepository recommendationRepository;

    @Transactional(readOnly = true)
    public List<ApartmentResponse> getPersonalizedRecommendations(Long userId) {
        return logRepository.findUserPreference(userId, LocalDateTime.now().minusDays(30))
                .map(pref -> {
                    log.info("선호도 계산됨: 지역={}, 가격={}, 월세={}, 면적={}, 전세여부={}, 월세여부={}",
                            pref.favoriteSggCode(),
                            pref.preferredPrice(),
                            pref.preferredMonthly(),
                            pref.preferredArea(),
                            pref.isRent(),
                            pref.isWolse());
                    return recommendationRepository.findRecommendedApartments(pref, userId);
                })
                .orElseGet(List::of);
    }

    public List<ApartmentResponse> getDefaultRecommendations() {
        return recommendationRepository.findDefaultRecommendations();
    }
}
