package org.example.homedatazip.listing.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.ListingErrorCode;
import org.example.homedatazip.listing.dto.ListingSearchResponse;
import org.example.homedatazip.listing.dto.MyListingResponse;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.listing.type.ListingStatus;
import org.example.homedatazip.listing.type.RentType;
import org.example.homedatazip.listing.type.TradeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                l.getCreatedAt()
        );
    }

    private MyListingResponse toMyListingResponse(Listing l) {

        Integer buildYear = null;
        try {
            buildYear = l.getApartment().getBuildYear();
        } catch (Exception ignored) {}

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
                l.getCreatedAt()
        );
    }
}
