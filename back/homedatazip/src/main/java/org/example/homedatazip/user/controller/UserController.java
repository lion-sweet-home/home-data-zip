package org.example.homedatazip.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.email.dto.EmailRequest;
import org.example.homedatazip.email.dto.EmailVerificationRequest;
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

    // 이메일 중복 확인
    @PostMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestBody @Valid EmailCheckRequest request) {

        return ResponseEntity.ok().body(userService.isEmailAvailable(request.email()));
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

    // 인증 코드 확인
    @PostMapping("/verify-email-code")
    public ResponseEntity<Void> verifyEmailCode(@RequestBody @Valid EmailVerificationRequest request) {
        emailAuthService.verifyCode(request.email(), request.authCode());
        return ResponseEntity.ok().build();
    }

    // 알림 수신 설정 변경
    @PutMapping("/notification-setting")
    public ResponseEntity<Void> updateNotificationSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody NotificationSettingRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.updateNotificationSetting(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    // 알림 수신 설정 조회
    @GetMapping("/notification-setting")
    public ResponseEntity<NotificationSettingResponse> getNotificationSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(userService.getNotificationSetting(userDetails.getUserId()));
    }

    @GetMapping("/{userId}/my-page")
    public ResponseEntity<MyPageResponse> getMyPage(@PathVariable Long userId,
                                                    @AuthenticationPrincipal CustomUserDetails user) {

        return ResponseEntity.status(HttpStatus.OK).body(userService.getMyPageInfo(userId, user.getEmail()));
    }

    @PatchMapping("/{userId}/my-page")
    public ResponseEntity<MyPageResponse> editMyPage(@PathVariable Long userId,
                                                     @AuthenticationPrincipal CustomUserDetails user,
                                                     @Valid @RequestBody MyPageEditRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.editMyPage(request,user.getEmail(),userId));
    }

    // 비밀번호 변경
    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {

        userService.changePassword(userDetails.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    // 유저 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                userService.getMe(userDetails.getUserId())
        );
    }
}
