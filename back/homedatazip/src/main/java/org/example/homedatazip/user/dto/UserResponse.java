package org.example.homedatazip.user.dto;

import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.UserRole;
import org.example.homedatazip.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long userId,
        String nickname,
        String email,
        LocalDateTime createdAt,
        List<String> roles
) {

    public static UserResponse from(User user) {
        List<String> roles = user.getRoles().stream()
                .map(UserRole::getRole)
                .map(r -> r.getRoleType().name())
                .toList();

        return new UserResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getCreatedAt(),
                roles
        );
    }
}
