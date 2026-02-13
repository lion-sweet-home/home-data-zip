package org.example.homedatazip.chat.repository;

import org.example.homedatazip.chat.dto.ChatRoomListResponse;
import org.example.homedatazip.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByBuyerIdAndListingId(Long buyerId, Long listingId);

    Optional<ChatRoom> findByListingId(Long listingId);

    @Query("""
        select count(cr) > 0
        from ChatRoom cr
        where cr.id = :roomId
        and(
            cr.buyer.email = :email
            or cr.listing.user.email = :email
        )
""")
    boolean existsParticipant(Long roomId, String email);

    // ChatRoom을 가져올때 구매자, 매물, 판매자,아파트 다 가져오도록 함.
    @Query("""
        select cr from ChatRoom cr
        join fetch cr.buyer
        join fetch cr.listing l
        join fetch l.user
        join fetch l.apartment
        where cr.id = :roomId
""")
    Optional<ChatRoom> findByIdWithUsers(Long roomId);

    // 나간 방을 제외하고 내가 있는 모든 방 조회
    @Query("""
        select cr from ChatRoom cr
        join fetch cr.listing l
        join fetch l.apartment
        where (cr.buyer.id = :userId and cr.buyerExited = false)
        or (l.user.id = :userId and cr.sellerExited = false)
        order by cr.lastMessageTime desc
""")
    List<ChatRoom> findAllByUserId(Long userId);
}
