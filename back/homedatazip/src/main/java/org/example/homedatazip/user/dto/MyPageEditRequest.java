package org.example.homedatazip.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MyPageEditRequest (
        @NotBlank
        @Size(min = 2, max = 30)
        String nickname,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
){}
