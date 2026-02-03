package org.example.homedatazip.favorite.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.user.entity.User;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "favorites",
        indexes = {
                @Index(name = "idx_favorite_user", columnList = "user_id") // 내 관심매물 목록 조회
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "listing_id"})
        }
)
public class Favorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Listing listing;

    private Favorite(User user, Listing listing) {
        this.user = user;
        this.listing = listing;
    }

    public static Favorite of(User user, Listing listing) {
        return new Favorite(user, listing);
    }
}
