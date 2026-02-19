package org.example.homedatazip.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneAuthSendRequest(
        @NotBlank
        @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "휴대폰 번호 형식이 올바르지 않습니다. 예) 01012345678")
        String phoneNumber
) {}
