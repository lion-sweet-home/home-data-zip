package org.example.homedatazip.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.email.dto.EmailRequest;
import org.example.homedatazip.email.service.EmailAuthService;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.user.dto.NicknameCheckRequest;
import org.example.homedatazip.user.dto.NotificationSettingRequest;
import org.example.homedatazip.user.dto.RegisterRequest;
import org.example.homedatazip.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/users")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailAuthService emailAuthService;

    // 닉네임 중복 확인
    @PostMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickname(@RequestBody @Valid NicknameCheckRequest request) {

        return ResponseEntity.ok().body(userService.isNicknameAvailable(request.nickname()));
    }

    // 회원 가입
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {
        userService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 인증 코드 발송
    @PostMapping("/email-verification")
    public ResponseEntity<Void> sendEmailCode(@RequestBody EmailRequest request) {
        emailAuthService.sendAuthCode(request.email());
        return ResponseEntity.ok().build();
    }

    // 알림 수신 설정 변경
    @PutMapping("/notification-setting")
    public ResponseEntity<Void> updateNotificationSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationSettingRequest request) {
        userService.updateNotificationSetting(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }
}
