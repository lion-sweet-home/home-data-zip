package org.example.homedatazip.notification.repository;

import org.example.homedatazip.notification.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByNotificationId(Long notificationId);

    // 전체 목록 조회 (삭제되지 않은 것만, 최신순)
    @Query("SELECT un FROM UserNotification un WHERE un.user.id = :userId ORDER BY un.createdAt DESC")
    List<UserNotification> findAllByUserId(@Param("userId") Long userId);

    // 읽음 목록 조회 (readAt이 null이 아닌 것, 최신순)
    @Query("SELECT un FROM UserNotification un WHERE un.user.id = :userId AND un.readAt IS NOT NULL ORDER BY un.createdAt DESC")
    List<UserNotification> findReadByUserId(@Param("userId") Long userId);

    // 미읽음 목록 조회 (readAt이 null인 것, 최신순)
    @Query("SELECT un FROM UserNotification un WHERE un.user.id = :userId AND un.readAt IS NULL ORDER BY un.createdAt DESC")
    List<UserNotification> findUnreadByUserId(@Param("userId") Long userId);

    // 사용자 알림 조회 (읽음 처리 시 사용)
    @Query("SELECT un FROM UserNotification un WHERE un.user.id = :userId AND un.id = :userNotificationId")
    java.util.Optional<UserNotification> findByUserIdAndId(@Param("userId") Long userId, @Param("userNotificationId") Long userNotificationId);
}
