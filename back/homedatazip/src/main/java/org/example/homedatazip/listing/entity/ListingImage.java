package org.example.homedatazip.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homedatazip.common.BaseTimeEntity;

@Entity
@Table(
        name = "listing_images",
        indexes = {
                @Index(name = "idx_listing_images_listing", columnList = "listing_id"),
                @Index(name = "idx_listing_images_main", columnList = "listing_id, is_main")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // ✅ builder 통해서만 만들도록 막고 싶으면 PRIVATE OK
@Builder(access = AccessLevel.PUBLIC)              // ✅ 근데 builder()는 PUBLIC 이어야 서비스에서 호출 가능
public class ListingImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Column(name = "is_main", nullable = false)
    private boolean main;

    @Column(name = "sort_order")
    private Integer sortOrder;

    // 양방향 세팅용
    void setListing(Listing listing) {
        this.listing = listing;
    }

    public void setMain(boolean main) {
        this.main = main;
    }
}
