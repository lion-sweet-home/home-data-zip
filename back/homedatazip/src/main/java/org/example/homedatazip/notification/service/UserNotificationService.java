package org.example.homedatazip.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.NotificationErrorCode;
import org.example.homedatazip.notification.dto.UserNotificationResponse;
import org.example.homedatazip.notification.entity.Notification;
import org.example.homedatazip.notification.entity.UserNotification;
import org.example.homedatazip.notification.repository.UserNotificationRepository;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterService sseEmitterService;

    // 알림 수신 설정한 유저들에게 공지사항 알림 전송
    @Transactional
    public void sendNotificationToUsers(Notification notification) {
        List<User> users = userRepository.findByNotificationEnabledTrue();

        List<UserNotification> userNotifications = users.stream()
                .map(user -> UserNotification.builder()
                        .user(user)
                        .notification(notification)
                        .build())
                .toList();
        List<UserNotification> savedNotifications = userNotificationRepository.saveAll(userNotifications);

        // SSE로 실시간 알림 전송
        savedNotifications.forEach(userNotification -> {
            UserNotificationResponse response = UserNotificationResponse.from(userNotification);
            sseEmitterService.sendNotification(
                    userNotification.getUser().getId(),
                    response
            );
        });
    }

    // 전체 조회
    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getAllNotifications(Long userId) {
        List<UserNotification> notifications = userNotificationRepository.findAllByUserId(userId);
        return notifications.stream()
                .map(UserNotificationResponse::from)
                .toList();
    }

    // 읽음 조회
    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getReadNotifications(Long userId) {
        List<UserNotification> notifications = userNotificationRepository.findReadByUserId(userId);
        return notifications.stream()
                .map(UserNotificationResponse::from)
                .toList();
    }

    // 미읽음 조회
    @Transactional(readOnly = true)
    public List<UserNotificationResponse> getUnreadNotifications(Long userId) {
        List<UserNotification> notifications = userNotificationRepository.findUnreadByUserId(userId);
        return notifications.stream()
                .map(UserNotificationResponse::from)
                .toList();
    }

    // 읽음 처리
    @Transactional
    public void markAsRead(Long userId, Long userNotificationId) {
        UserNotification userNotification = userNotificationRepository.findByUserIdAndId(userId, userNotificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.USER_NOTIFICATION_NOT_FOUND));

        // 이미 읽은 경우는 그대로 반환
        if (userNotification.getReadAt() == null) {
            userNotification.setReadAt(LocalDateTime.now());
        }
    }

    // 전체 읽음 처리
    @Transactional
    public void markAllAsRead(Long userId) {
        List<UserNotification> unreadNotifications = userNotificationRepository.findUnreadByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        
        unreadNotifications.forEach(notification -> {
            if (notification.getReadAt() == null) {
                notification.setReadAt(now);
            }
        });
    }

    // 알림 삭제
    @Transactional
    public void deleteUserNotification(Long userId, Long userNotificationId) {
        UserNotification userNotification = userNotificationRepository.findByUserIdAndId(userId, userNotificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.USER_NOTIFICATION_NOT_FOUND));

        userNotificationRepository.delete(userNotification);
    }

    // 읽은 알림 전체 삭제
    @Transactional
    public void deleteAllReadNotifications(Long userId) {
        List<UserNotification> readNotifications = userNotificationRepository.findReadByUserId(userId);
        userNotificationRepository.deleteAll(readNotifications);
    }
}
