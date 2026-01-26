package org.example.homedatazip.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.notification.dto.NotificationRequest;
import org.example.homedatazip.notification.dto.NotificationResponse;
import org.example.homedatazip.notification.entity.Notification;
import org.example.homedatazip.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    // TODO: UserDetails로 변경 예정

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
                .title(request.title())
                .message(request.message())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
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
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없음"));

        notification.setTitle(request.title());
        notification.setMessage(request.message());

        Notification updatedNotification = notificationRepository.save(notification);
        return NotificationResponse.from(updatedNotification);
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없음"));

        notificationRepository.delete(notification);
    }
}
