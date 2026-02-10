package org.example.homedatazip.listing.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.ListingErrorCode;
import org.example.homedatazip.listing.dto.ListingCreateRequest;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.listing.type.TradeType;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Region region = apartment.getRegion();

        validateCreate(req);

        if (req.tradeType() == TradeType.SALE) {
            return listingRepository.save(
                    Listing.createSale(
                            user, region, apartment,
                            req.exclusiveArea(),
                            req.floor(),
                            req.salePrice(),
                            req.contactPhone(),
                            req.description()
                    )
            );
        }

        return listingRepository.save(
                Listing.createRent(
                        user, region, apartment,
                        req.exclusiveArea(),
                        req.floor(),
                        req.deposit(),
                        req.monthlyRent(),
                        req.contactPhone(),
                        req.description()
                )
        );
    }


     // 수정 (추후 작성)
    @Transactional
    public void update(Long userId, Long listingId /*, ListingUpdateRequest req */) {

    }


     // 삭제(soft delete)
    @Transactional
    public void delete(Long userId, Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException(ListingErrorCode.LISTING_NOT_FOUND));

        if (!listing.getUser().getId().equals(userId)) {
            throw new BusinessException(ListingErrorCode.FORBIDDEN);
        }

        listing.delete();
    }

    private void validateCreate(ListingCreateRequest req) {

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
}
