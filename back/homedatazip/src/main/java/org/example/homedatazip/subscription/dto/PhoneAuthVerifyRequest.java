package org.example.homedatazip.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneAuthVerifyRequest(
        @NotBlank
        @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "휴대폰 번호 형식이 올바르지 않습니다. 예) 01012345678")
        String phoneNumber,

        @NotBlank
        String requestId,

        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.")
        String code
) {}
