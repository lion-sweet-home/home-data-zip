package org.example.homedatazip.recommend.repository;

import org.example.homedatazip.recommend.dto.UserPreference;
import org.example.homedatazip.recommend.entity.UserSearchLog;
import org.example.homedatazip.recommend.type.LogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserSearchLogRepository extends JpaRepository<UserSearchLog, Long> {

    Optional<UserSearchLog> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserSearchLog> findTopByUserIdAndLogTypeOrderByCreatedAtDesc(Long userId, LogType logType);

    @Query("""
    SELECT new org.example.homedatazip.recommend.dto.UserPreference(
        l.sggCode,
        SUM(CASE WHEN l.finalPrice > 0 THEN l.finalPrice * l.score ELSE 0L END), 
        SUM(CASE WHEN l.monthlyRent > 0 THEN l.monthlyRent * l.score ELSE 0L END), 
        SUM(CASE WHEN l.finalArea > 0 THEN l.finalArea * l.score ELSE 0.0 END),
        SUM(CASE WHEN l.finalPrice > 0 THEN CAST(l.score AS long) ELSE 0L END), 
        SUM(CASE WHEN l.monthlyRent > 0 THEN CAST(l.score AS long) ELSE 0L END), 
        SUM(CASE WHEN l.finalArea > 0 THEN CAST(l.score AS long) ELSE 0L END), 
        SUM(CASE WHEN l.tradeType IN (org.example.homedatazip.recommend.type.TradeType.RENT, org.example.homedatazip.recommend.type.TradeType.WOLSE) THEN CAST(l.score AS long) ELSE 0L END), 
        SUM(CASE WHEN l.tradeType = org.example.homedatazip.recommend.type.TradeType.SALE THEN CAST(l.score AS long) ELSE 0L END), 
        SUM(CASE WHEN l.tradeType = org.example.homedatazip.recommend.type.TradeType.WOLSE THEN CAST(l.score AS long) ELSE 0L END), 
        SUM(CASE WHEN l.tradeType = org.example.homedatazip.recommend.type.TradeType.RENT THEN CAST(l.score AS long) ELSE 0L END) 
    )
    FROM UserSearchLog l
    WHERE l.userId = :userId
      AND l.createdAt > :since
    GROUP BY l.sggCode
    ORDER BY SUM(l.score) DESC
    LIMIT 1
""")
    Optional<UserPreference> findUserPreference(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );
}