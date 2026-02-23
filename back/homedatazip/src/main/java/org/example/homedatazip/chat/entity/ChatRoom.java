package org.example.homedatazip.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.user.entity.User;

import java.time.LocalDateTime;

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

    // 구매자, 판매자 퇴장 여부
    @Builder.Default
    private boolean buyerExited = false;

    @Builder.Default
    private boolean sellerExited = false;

    // 구매자, 판매자 입장 여부
    @Builder.Default
    private boolean buyerEntered = false;

    @Builder.Default
    private boolean sellerEntered = false;

    @Column()
    private String lastMessage; // 마지막 메시지 내용

    @Column()
    private LocalDateTime lastMessageTime; // 마지막 메시지 시간

    public static ChatRoom create(User buyer, Listing listing) {
        return ChatRoom.builder()
                .buyer(buyer)
                .listing(listing)
                .build();
    }

    public void updateLastMessage(String lastMessage, LocalDateTime lastMessageTime) {
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public void exitBuyer() {
        this.buyerExited = true;
        this.buyerEntered = false;
    }

    public void exitSeller() {
        this.sellerExited = true;
        this.sellerEntered = false;
    }

    public void enterBuyer() {
        this.buyerEntered = true;
        this.buyerExited = false;
    }

    public void enterSeller() {
        this.sellerEntered = true;
        this.sellerExited = false;
    }

    public boolean isBuyer(Long userId) {
        return buyer.getId().equals(userId);
    }

    public boolean isSeller(Long userId) {
        return listing.getUser().getId().equals(userId);
    }

    public boolean isBuyerAndNotEntered(Long userId) {
        return isBuyer(userId) && !this.buyerEntered;
    }

    public boolean isSellerAndNotEntered(Long userId) {
        return isSeller(userId) && !this.sellerEntered;
    }
}
