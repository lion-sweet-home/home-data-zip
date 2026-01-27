package org.example.homedatazip.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.NotificationErrorCode;
import org.example.homedatazip.notification.dto.NotificationRequest;
import org.example.homedatazip.notification.dto.NotificationResponse;
import org.example.homedatazip.notification.entity.Notification;
import org.example.homedatazip.notification.entity.UserNotification;
import org.example.homedatazip.notification.repository.NotificationRepository;
import org.example.homedatazip.notification.repository.UserNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationService userNotificationService;
    private final UserNotificationRepository userNotificationRepository;

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
                .title(request.title())
                .message(request.message())
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        userNotificationService.sendNotificationToUsers(savedNotification);

        return NotificationResponse.from(savedNotification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications() {
        List<Notification> notifications = notificationRepository.findAll();
        return notifications.stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse updateNotification(Long notificationId, NotificationRequest request) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.setTitle(request.title());
        notification.setMessage(request.message());

        Notification updatedNotification = notificationRepository.save(notification);
        return NotificationResponse.from(updatedNotification);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        List<UserNotification> userNotifications = userNotificationRepository.findByNotificationId(notificationId);
        userNotificationRepository.deleteAll(userNotifications);

        notificationRepository.delete(notification);
    }
}
