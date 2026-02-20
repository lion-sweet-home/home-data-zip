package org.example.homedatazip.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserMeResponse(
        Long id,
        String email,
        String nickname,
        String phoneNumber,
        boolean phoneVerified,
        LocalDateTime phoneVerifiedAt,
        List<String> roles
) {}