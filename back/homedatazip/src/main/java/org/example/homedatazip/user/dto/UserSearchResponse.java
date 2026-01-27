package org.example.homedatazip.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.homedatazip.user.entity.User;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
public class UserSearchResponse {

    private String nickname;
    private String email;
    private LocalDateTime createdAt;

    public static UserSearchResponse create(User user) {
        return UserSearchResponse.builder()
                .nickname(user.getNickname())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
