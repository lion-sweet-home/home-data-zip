package org.example.homedatazip.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.user.entity.User;

@Entity
@Getter
@Table(name = "chat_rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"buyer_id", "listing_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Builder.Default
    private boolean buyerExited = false;

    @Builder.Default
    private boolean sellerExited = false;

    public static ChatRoom create(User buyer, Listing listing) {
        return ChatRoom.builder()
                .buyer(buyer)
                .listing(listing)
                .build();
    }
}
