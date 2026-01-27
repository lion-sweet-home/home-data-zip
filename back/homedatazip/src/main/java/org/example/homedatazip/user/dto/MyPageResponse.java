package org.example.homedatazip.user.dto;

import org.example.homedatazip.user.entity.User;

public record MyPageResponse (
        String email,
        String Nickname
){
    public static MyPageResponse from(User user){
        return new MyPageResponse(
                user.getEmail(),
                user.getNickname()
        );
    }
}
