package org.example.homedatazip.listing.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.ListingErrorCode;
import org.example.homedatazip.listing.dto.ListingDetailResponse;
import org.example.homedatazip.listing.dto.ListingImageResponse;
import org.example.homedatazip.listing.dto.ListingSearchResponse;
import org.example.homedatazip.listing.dto.MyListingResponse;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.entity.ListingImage;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.listing.type.ListingStatus;
import org.example.homedatazip.listing.type.RentType;
import org.example.homedatazip.listing.type.TradeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingQueryService {

    private final ListingRepository listingRepository;

    /**
     * 전체/필터 검색 (ACTIVE만)
     */
    @Transactional(readOnly = true)
    public List<ListingSearchResponse> search(
            Long regionId,
            Long apartmentId,
            TradeType tradeType,
            RentType rentType,
            int limit
    ) {
        List<Listing> list =
                listingRepository.searchActive(regionId, apartmentId, tradeType, limit);

        // RENT + rentType 필터
        if (tradeType == TradeType.RENT && rentType != null) {
            if (rentType == RentType.CHARTER) {
                list = list.stream()
                        .filter(l -> l.getMonthlyRent() != null && l.getMonthlyRent() == 0)
                        .toList();
            } else if (rentType == RentType.MONTHLY) {
                list = list.stream()
                        .filter(l -> l.getMonthlyRent() != null && l.getMonthlyRent() >= 1)
                        .toList();
            }
        }

        if (list.size() > limit) {
            list = list.subList(0, limit);
        }

        return list.stream()
                .map(this::toSearchResponse)
                .toList();
    }

    /**
     * 내 매물 조회
     */
    @Transactional(readOnly = true)
    public List<MyListingResponse> myListings(Long userId, String status) {

        List<Listing> list;

        if (status == null || status.isBlank()) {
            list = listingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else {
            ListingStatus st;
            try {
                st = ListingStatus.valueOf(status);
            } catch (Exception e) {
                throw new BusinessException(ListingErrorCode.INVALID_STATUS);
            }
            list = listingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, st);
        }

        return list.stream()
                .map(this::toMyListingResponse)
                .toList();
    }

    private ListingSearchResponse toSearchResponse(Listing l) {

        Integer buildYear = null;
        try {
            buildYear = l.getApartment().getBuildYear();
        } catch (Exception ignored) {}

        RentType rentType = null;
        if (l.getTradeType() == TradeType.RENT) {
            rentType = (l.getMonthlyRent() != null && l.getMonthlyRent() == 0)
                    ? RentType.CHARTER
                    : RentType.MONTHLY;
        }

        String mainImageUrl = extractMainImageUrl(l);

        return new ListingSearchResponse(
                l.getId(),
                l.getRegion().getId(),
                l.getApartment().getId(),
                l.getApartment().getAptName(),
                buildYear,
                l.getTradeType(),
                rentType,
                l.getExclusiveArea(),
                l.getFloor(),
                l.getSalePrice(),
                l.getDeposit(),
                l.getMonthlyRent(),
                l.getDescription(),
                l.getCreatedAt(),
                mainImageUrl
        );
    }

    private MyListingResponse toMyListingResponse(Listing l) {

        Integer buildYear = null;
        try {
            buildYear = l.getApartment().getBuildYear();
        } catch (Exception ignored) {}

        String mainImageUrl = extractMainImageUrl(l);

        return new MyListingResponse(
                l.getId(),
                l.getApartment().getId(),
                l.getApartment().getAptName(),
                buildYear,
                l.getTradeType(),
                l.getExclusiveArea(),
                l.getFloor(),
                l.getSalePrice(),
                l.getDeposit(),
                l.getMonthlyRent(),
                l.getContactPhone(),
                l.getDescription(),
                l.getStatus().name(),
                l.getCreatedAt(),
                mainImageUrl
        );
    }

    /**
     * 대표 이미지 URL 뽑기 규칙
     * 1) isMain=true 우선
     * 2) sortOrder 낮은 거 우선
     * 3) id 낮은 거 우선
     */
    private String extractMainImageUrl(Listing l) {
        if (l.getImages() == null || l.getImages().isEmpty()) return null;

        return l.getImages().stream()
                .sorted(Comparator
                        .comparing(ListingImage::isMain).reversed()
                        .thenComparing(img -> img.getSortOrder() == null ? Integer.MAX_VALUE : img.getSortOrder())
                        .thenComparing(img -> img.getId() == null ? Long.MAX_VALUE : img.getId())
                )
                .map(ListingImage::getUrl)
                .findFirst()
                .orElse(null);
    }

    /**
     *  상세조회
     * - 삭제된(DELETED) 매물은 404 처리
     */
    @Transactional(readOnly = true)
    public ListingDetailResponse detail(Long listingId) {
        Listing l = listingRepository.findDetailWithImages(listingId)
                .orElseThrow(() -> new BusinessException(ListingErrorCode.LISTING_NOT_FOUND));

        // 여기서 ACTIVE 체크 (B안 핵심)
        if (l.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessException(ListingErrorCode.LISTING_NOT_FOUND);
        }

        String title = l.getApartment().getAptName();

        List<ListingImageResponse> images =
                (l.getImages() == null ? List.<ListingImageResponse>of() :
                        l.getImages().stream()
                                .sorted(Comparator
                                        .comparing(ListingImage::isMain).reversed()
                                        .thenComparing(img -> img.getSortOrder() == null ? Integer.MAX_VALUE : img.getSortOrder())
                                        .thenComparing(img -> img.getId() == null ? Long.MAX_VALUE : img.getId())
                                )
                                .map(img -> new ListingImageResponse(
                                        img.getId(),
                                        img.getUrl(),
                                        img.isMain(),
                                        img.getSortOrder()
                                ))
                                .toList()
                );

        return new ListingDetailResponse(l.getId(), title, images);
    }
}
