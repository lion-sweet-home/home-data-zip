package org.example.homedatazip.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "닉네임을 입력해 주세요")
        @Size(min = 2, max = 30, message = "닉네임은 2~30 자여야합니다.")
        String nickname,

        @NotBlank(message = "이메일을 입력하세요")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 50, message = "비밀번호는 8~50 자여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*[@$!%*#?&]).+$",
                message = "비밀번호는 영문과 특수문자를 최소 1개씩 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "인증 코드를 입력해 주세요")
        String authCode
) {

}
