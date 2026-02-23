package org.example.homedatazip.chat.repository;

import org.example.homedatazip.chat.entity.ChatMessage;
import org.example.homedatazip.chat.entity.ChatRoom;
import org.example.homedatazip.chat.entity.MessageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 채팅방의 메시지를 최신순으로 페이징 조회
    @Query("""
        select cm from ChatMessage cm
        join fetch cm.sender
        where cm.chatRoom.id = :chatRoomId
""")
    Slice<ChatMessage> findByChatRoomId(Long chatRoomId, Pageable pageable);

    // 내가 읽지 않은 메시지가 총 몇 개인지 반환, TALK 타입만
    @Query("""
        select count(cm) from ChatMessage cm
        where (cm.chatRoom.buyer.id = :userId or cm.chatRoom.listing.user.id = :userId)
        and cm.sender.id != :userId
        and cm.isRead = false
        and cm.type = :type
""")
    long countTotalUnreadMessages(Long userId, MessageType type);

    // 특정 채팅방에 입장했을 때 받은 메시지들 읽음 처리
    @Modifying(clearAutomatically = true)
    @Query("""
        update ChatMessage cm set cm.isRead = true
        where cm.chatRoom.id = :roomId
        and cm.sender.id != :userId
        and cm.isRead = false
""")
    void markAsReadByRoomIdAndReceiverId(Long roomId, Long userId);

    // 특정 방에서 내가 안읽은 메시지 개수 조회, TALK 타입만
    long countByChatRoomAndIsReadFalseAndSenderIdNotAndType(
            ChatRoom chatRoom,
            Long senderId,
            MessageType type);

}
