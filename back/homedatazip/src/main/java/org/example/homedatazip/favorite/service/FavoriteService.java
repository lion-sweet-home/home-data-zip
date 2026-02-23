package org.example.homedatazip.favorite.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.favorite.dto.FavoriteListingResponse;
import org.example.homedatazip.favorite.entity.Favorite;
import org.example.homedatazip.favorite.repository.FavoriteRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.FavoriteErrorCode;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    /** 관심 매물 등록 */
    @Transactional
    public void add(Long userId, Long listingId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException(FavoriteErrorCode.LISTING_NOT_FOUND));

        if (favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            throw new BusinessException(FavoriteErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        Favorite favorite = Favorite.of(user, listing);
        favoriteRepository.save(favorite);
    }

    /** 관심 매물 해제 */
    @Transactional
    public void remove(Long userId, Long listingId) {
        Favorite favorite = favoriteRepository.findByUserIdAndListingId(userId, listingId)
                .orElseThrow(() -> new BusinessException(FavoriteErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    /** 관심 매물 목록 전체 (관심 등록 시점 최신순) */
    @Transactional(readOnly = true)
    public List<FavoriteListingResponse> getMyFavorites(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDescWithListingAndApartment(userId);
        return favorites.stream()
                .map(this::toFavoriteListingResponse)
                .toList();
    }

    // 관심 매물 정보 변환
    private FavoriteListingResponse toFavoriteListingResponse(Favorite f) {
        Listing listing = f.getListing();
        Apartment apt = listing.getApartment();
        return new FavoriteListingResponse(
                f.getId(),
                listing.getId(),
                apt.getAptName(),
                apt.getRoadAddress(),
                apt.getJibunAddress(),
                listing.getTradeType(),
                listing.getExclusiveArea(),
                listing.getFloor(),
                listing.getSalePrice(),
                listing.getDeposit(),
                listing.getMonthlyRent(),
                listing.getContactPhone(),
                f.getCreatedAt()
        );
    }
}
