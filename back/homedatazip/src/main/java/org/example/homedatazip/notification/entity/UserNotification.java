package org.example.homedatazip.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_notifications",
        indexes = {
                @Index(name = "idx_user_created", columnList = "user_id, createdAt"), // 알림 목록
                @Index(name = "idx_user_read", columnList = "user_id, readAt") // 알림 미읽음 개수
        })
public class UserNotification extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    private LocalDateTime readAt;

    @Column(nullable = false)
    private boolean isDeleted = false;
}