package org.example.homedatazip.chat.repository;

import org.example.homedatazip.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByBuyerIdAndListingId(Long buyerId, Long listingId);
}
