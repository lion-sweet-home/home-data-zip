package org.example.homedatazip.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.email.dto.EmailRequest;
import org.example.homedatazip.email.service.EmailAuthService;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.user.dto.*;
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
    // TODO: UserDetails로 변경 예정 (인증된 사용자의 userId 사용)
    @PutMapping("/{userId}/notification-setting")
    public ResponseEntity<Void> updateNotificationSetting(
            @PathVariable Long userId,
            @Valid @RequestBody NotificationSettingRequest request) {
        userService.updateNotificationSetting(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/my-page")
    public ResponseEntity<MyPageResponse> getMyPage(@PathVariable Long userId,
                                                    @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.status(HttpStatus.OK).body(userService.getMyPageInfo(userId, user.getEmail()));
    }

    @PatchMapping("/{userId}/my-page/edit")
    public ResponseEntity<MyPageResponse> editMyPage(@PathVariable Long userId,
                                                     @AuthenticationPrincipal CustomUserDetails user,
                                                     @Valid @RequestBody MyPageEditRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.editMyPage(request,user.getEmail(),userId));
    }
}
