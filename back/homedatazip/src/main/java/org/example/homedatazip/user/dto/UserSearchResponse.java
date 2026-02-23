package org.example.homedatazip.user.dto;

import lombok.AccessLevel;
import lombok.Builder;
import org.example.homedatazip.user.entity.User;

import java.time.LocalDateTime;

@Builder(access = AccessLevel.PRIVATE)
public record UserSearchResponse(
        Long userId,
        String nickname,
        String email,
        LocalDateTime createdAt
) {
    public static UserSearchResponse create(User user) {
        return UserSearchResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
