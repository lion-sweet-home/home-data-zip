package org.example.homedatazip.test.ai.repository;

import org.example.homedatazip.test.ai.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    List<AiMessage> findByUserIdAndSessionIdOrderByIdAsc(Long userId, String sessionId);

    @Query("SELECT DISTINCT sessionId FROM AiMessage WHERE user.id = :userId")
    List<String> findDistinctSessionIdsByUserId(Long userId);

}
