package org.example.homedatazip.listing.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.data.Region;
import org.example.homedatazip.listing.type.ListingStatus;
import org.example.homedatazip.listing.type.TradeType;
import org.example.homedatazip.user.entity.User;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "listing",
        indexes = {
                @Index(name = "idx_listing_user_status", columnList = "user_id, status"),
                @Index(name = "idx_listing_region_type", columnList = "region_id, trade_type, status"),
                @Index(name = "idx_listing_apartment", columnList = "apartment_id, status")
        }
)
public class Listing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 10)
    private TradeType tradeType;

    @Column(name = "exclusive_area", nullable = false)
    private Double exclusiveArea;

    @Column(name = "floor", nullable = false)
    private Integer floor;

    @Column(name = "sale_price")
    private Long salePrice;

    @Column(name = "deposit")
    private Long deposit;

    @Column(name = "monthly_rent")
    private Integer monthlyRent;

    @Column(name = "contact_phone", nullable = true, length = 30)
    private String contactPhone;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ListingStatus status;

    private Listing(
            User user,
            Region region,
            Apartment apartment,
            TradeType tradeType,
            Double exclusiveArea,
            Integer floor,
            Long salePrice,
            Long deposit,
            Integer monthlyRent,
            String contactPhone,
            String description
    ) {
        this.user = user;
        this.region = region;
        this.apartment = apartment;
        this.tradeType = tradeType;
        this.exclusiveArea = exclusiveArea;
        this.floor = floor;
        this.salePrice = salePrice;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.contactPhone = contactPhone;
        this.description = description;
        this.status = ListingStatus.ACTIVE;
    }

    public static Listing createSale(
            User user,
            Region region,
            Apartment apartment,
            Double exclusiveArea,
            Integer floor,
            Long salePrice,
            String contactPhone,
            String description
    ) {
        return new Listing(
                user,
                region,
                apartment,
                TradeType.SALE,
                exclusiveArea,
                floor,
                salePrice,
                null,
                null,
                contactPhone,
                description
        );
    }

    public static Listing createRent(
            User user, Region region, Apartment apartment,
            Double exclusiveArea, Integer floor,
            Long deposit, Integer monthlyRent,
            String contactPhone, String description
    ) {
        return new Listing(
                user, region, apartment,
                TradeType.RENT,
                exclusiveArea, floor,
                null, deposit, monthlyRent,
                contactPhone, description
        );
    }

    public void update(
            Region region,
            Apartment apartment,
            Double exclusiveArea,
            Integer floor,
            Long salePrice,
            Long deposit,
            Integer monthlyRent,
            String contactPhone,
            String description
    ) {
        this.region = region;
        this.apartment = apartment;
        this.exclusiveArea = exclusiveArea;
        this.floor = floor;
        this.salePrice = salePrice;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.contactPhone = contactPhone;
        this.description = description;
    }

    public void delete() {
        this.status = ListingStatus.DELETED;
    }

    //S3 이미지 삽입
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingImage> images = new ArrayList<>();

    public void addImage(ListingImage image) {
        this.images.add(image);
        image.setListing(this);
    }


    public void clearImages() {
        for (ListingImage img : images) {
            img.setListing(null);
        }
        images.clear();
    }
}
