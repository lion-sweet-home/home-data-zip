package org.example.homedatazip.listing.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.apartment.repository.ApartmentRepository;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.global.storage.s3.S3UploadService;
import org.example.homedatazip.listing.dto.ListingCreateRequest;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.listing.entity.ListingImage;
import org.example.homedatazip.listing.repository.ListingRepository;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static reactor.netty.http.HttpConnectionLiveness.log;

@Service
@RequiredArgsConstructor
public class ListingCommandService {

    private final ListingRepository listingRepository;
    private final S3UploadService s3UploadService;

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;

    @Transactional
    public Long create(Long userId, ListingCreateRequest request) {

        System.out.println(">>> ListingCommandService.create() IN");
        System.out.println(">>> tempKeys = " + request.imageTempKeys());
        System.out.println(">>> originalNames = " + request.imageOriginalNames());
        System.out.println(">>> mainIndex = " + request.mainIndex());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        Apartment apartment = apartmentRepository.findById(request.apartmentId())
                .orElseThrow(() -> new IllegalArgumentException("apartment not found"));

        Region region = apartment.getRegion();

        Listing listing;
        if (request.tradeType().name().equals("SALE")) {
            if (request.salePrice() == null || request.salePrice() <= 0) {
                throw new IllegalArgumentException("salePrice is required for SALE");
            }
            listing = Listing.createSale(
                    user,
                    region,
                    apartment,
                    request.exclusiveArea(),
                    request.floor(),
                    request.salePrice(),
                    request.contactPhone(),
                    request.description()
            );
        } else { // RENT
            if (request.deposit() == null || request.monthlyRent() == null) {
                throw new IllegalArgumentException("deposit/monthlyRent are required for RENT");
            }
            listing = Listing.createRent(
                    user,
                    region,
                    apartment,
                    request.exclusiveArea(),
                    request.floor(),
                    request.deposit(),
                    request.monthlyRent(),
                    request.contactPhone(),
                    request.description()
            );
        }

        // 4) 먼저 저장해서 listingId 확보
        listingRepository.save(listing);

        // 5) 이미지 붙이기 (temp -> listing 폴더로 move)
        attachImagesOnCreate(listing, request.imageTempKeys(), request.imageOriginalNames(), request.mainIndex());

        return listing.getId();
    }

    private void attachImagesOnCreate(
            Listing listing,
            List<String> tempKeys,
            List<String> originalNames,
            Integer mainIndex
    ) {
        System.out.println("### attachImagesOnCreate called. listingId=" + listing.getId());
        System.out.println("### tempKeys=" + tempKeys);
        System.out.println("### originalNames=" + originalNames);

        if (tempKeys == null || tempKeys.isEmpty()) return;

        if (originalNames == null || originalNames.size() != tempKeys.size()) {
            throw new IllegalArgumentException("imageOriginalNames size must match imageTempKeys size");
        }

        int mainIdx = (mainIndex == null ? 0 : mainIndex);

        for (int i = 0; i < tempKeys.size(); i++) {
            String tempKey = tempKeys.get(i);
            String originalName = originalNames.get(i);

            // S3 temp -> listing 으로 이동 (copy + delete)
            String finalKey = s3UploadService.moveToListing(listing.getId(), tempKey, originalName);

            // publicUrl 만들기
            String url = s3UploadService.publicUrl(finalKey);

            boolean isMain = (i == mainIdx);


            ListingImage img = ListingImage.builder()
                    .s3Key(finalKey)
                    .url(url)
                    .main(isMain)
                    .sortOrder(i)
                    .build();

            //  양방향 세팅은 Listing.addImage()에서 처리
            listing.addImage(img);
        }
    }
}
