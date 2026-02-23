package org.example.homedatazip.listing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.storage.s3.S3UploadService;
import org.example.homedatazip.listing.dto.ListingCreateRequest;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.entity.ListingImage;
import org.example.homedatazip.listing.repository.ListingImageRepository;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.listing.type.TradeType;
import org.example.homedatazip.global.exception.domain.ListingErrorCode;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingImageRepository listingImageRepository;
    private final S3UploadService s3UploadService;

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

        // 1) Listing 생성
        Listing listing = (req.tradeType() == TradeType.SALE)
                ? Listing.createSale(
                user, region, apartment,
                req.exclusiveArea(),
                req.floor(),
                req.salePrice(),
                req.contactPhone(),
                req.description()
        )
                : Listing.createRent(
                user, region, apartment,
                req.exclusiveArea(),
                req.floor(),
                req.deposit(),
                req.monthlyRent(),
                req.contactPhone(),
                req.description()
        );

        // 2) 먼저 저장해서 listingId 확보
        listingRepository.save(listing);

        // 3) 이미지 저장 (temp -> listing 이동 + listing_images insert)
        attachImagesOnCreate(listing, req.imageTempKeys(), req.imageOriginalNames(), req.mainIndex());

        return listing;
    }

    private void attachImagesOnCreate(
            Listing listing,
            List<String> tempKeys,
            List<String> originalNames,
            Integer mainIndex
    ) {
        if (tempKeys == null || tempKeys.isEmpty()) {
            log.info("[LISTING] no images. listingId={}", listing.getId());
            return;
        }

        if (originalNames == null || originalNames.size() != tempKeys.size()) {
            throw new BusinessException(ListingErrorCode.IMAGE_META_MISMATCH);

        }

        int mainIdx = (mainIndex == null ? 0 : mainIndex);
        if (mainIdx < 0 || mainIdx >= tempKeys.size()) {
            mainIdx = 0;
        }

        List<ListingImage> images = new ArrayList<>();

        for (int i = 0; i < tempKeys.size(); i++) {
            String tempKey = tempKeys.get(i);
            String originalName = originalNames.get(i);


            String finalKey = s3UploadService.moveToListing(listing.getId(), tempKey, originalName);


            String url = s3UploadService.publicUrl(s3UploadService.getBucket(), finalKey);

            boolean isMain = (i == mainIdx);

            ListingImage img = ListingImage.builder()
                    .listing(listing)
                    .s3Key(finalKey)
                    .url(url)
                    .main(isMain)
                    .sortOrder(i)
                    .build();

            images.add(img);
        }

        listingImageRepository.saveAll(images);

        log.info("[LISTING] images saved. listingId={}, count={}", listing.getId(), images.size());
    }


    // 수정 (추후 작성)
    @Transactional
    public void update(Long userId, Long listingId /*, ListingUpdateRequest req */) { }

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
