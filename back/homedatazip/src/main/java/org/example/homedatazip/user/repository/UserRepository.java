package org.example.homedatazip.user.repository;

import org.example.homedatazip.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // 알림 수신 설정한 사용자 조회
    List<User> findByNotificationEnabledTrue();
}
