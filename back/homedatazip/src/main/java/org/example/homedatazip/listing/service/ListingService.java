package org.example.homedatazip.listing.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.ListingErrorCode;
import org.example.homedatazip.listing.dto.ListingCreateRequest;
import org.example.homedatazip.listing.dto.MyListingResponse;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.listing.type.ListingStatus;
import org.example.homedatazip.listing.type.TradeType;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public Listing create(Long userId, ListingCreateRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ListingErrorCode.USER_NOT_FOUND));

        Apartment apartment = apartmentRepository.findById(req.apartmentId())
                .orElseThrow(() -> new BusinessException(ListingErrorCode.APARTMENT_NOT_FOUND));

        Region region = apartment.getRegion(); // regionId는 프론트가 안 줘도 됨

        validate(req);

        if (req.tradeType() == TradeType.SALE) {
            return listingRepository.save(
                    Listing.createSale(
                            user,
                            region,
                            apartment,
                            req.exclusiveArea(),
                            req.floor(),
                            req.salePrice(),
                            req.contactPhone(),
                            req.description()
                    )
            );
        }

        // RENT: 전세면 monthlyRent=0, 월세면 1 이상
        return listingRepository.save(
                Listing.createRent(
                        user,
                        region,
                        apartment,
                        req.exclusiveArea(),
                        req.floor(),
                        req.deposit(),
                        req.monthlyRent(),
                        req.contactPhone(),
                        req.description()
                )
        );
    }

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

    private void validate(ListingCreateRequest req) {

        if (req.tradeType() == null) throw new BusinessException(ListingErrorCode.TRADE_TYPE_REQUIRED);


        if (req.tradeType() == TradeType.SALE) {
            if (req.salePrice() == null || req.salePrice() <= 0) {
                throw new BusinessException(ListingErrorCode.SALE_PRICE_REQUIRED);
            }
            return;
        }

        // RENT
        if (req.deposit() == null || req.deposit() <= 0) {
            throw new BusinessException(ListingErrorCode.DEPOSIT_REQUIRED);
        }
        if (req.monthlyRent() == null || req.monthlyRent() < 0) {
            throw new BusinessException(ListingErrorCode.MONTHLY_RENT_INVALID);
        }
    }

    private MyListingResponse toMyListingResponse(Listing l) {

        Integer buildYear = null;

        try {
            buildYear = l.getApartment().getBuildYear();
        } catch (Exception ignored) {
            // 필드 없거나 getter 없으면 null 유지
        }

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
