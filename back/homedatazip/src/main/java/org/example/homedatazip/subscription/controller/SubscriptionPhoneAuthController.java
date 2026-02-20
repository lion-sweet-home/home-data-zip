package org.example.homedatazip.subscription.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.subscription.dto.*;
import org.example.homedatazip.subscription.service.SubscriptionPhoneAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions/phone-auth")
public class SubscriptionPhoneAuthController {

    private final SubscriptionPhoneAuthService phoneAuthService;

    @PostMapping("/send")
    public ResponseEntity<PhoneAuthSendResponse> send(@Valid @RequestBody PhoneAuthSendRequest request) {
        return ResponseEntity.ok(phoneAuthService.sendCode(request.phoneNumber()));
    }

    @PostMapping("/verify")
    public ResponseEntity<PhoneAuthVerifyResponse> verify(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody PhoneAuthVerifyRequest req
    ) {
        return ResponseEntity.ok(
                phoneAuthService.verifyCode(principal.getUserId(), req.phoneNumber(), req.requestId(), req.code())
        );
    }
}
