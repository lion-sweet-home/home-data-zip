package org.example.homedatazip.favorite.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.favorite.dto.FavoriteListingResponse;
import org.example.homedatazip.favorite.service.FavoriteService;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** 관심 매물 등록 */
    @PostMapping("/{listingId}")
    public ResponseEntity<Void> add(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long listingId
    ) {
        Long userId = userDetails.getUserId();
        favoriteService.add(userId, listingId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** 관심 매물 해제 */
    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long listingId
    ) {
        Long userId = userDetails.getUserId();
        favoriteService.remove(userId, listingId);
        return ResponseEntity.noContent().build();
    }

    /** 관심 매물 목록 전체 (관심 등록 시점 최신순) */
    @GetMapping
    public ResponseEntity<List<FavoriteListingResponse>> getMyFavorites(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        List<FavoriteListingResponse> favorites = favoriteService.getMyFavorites(userId);
        return ResponseEntity.ok(favorites);
    }
}
