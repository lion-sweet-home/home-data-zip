package org.example.homedatazip.subscription.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.subscription.dto.*;
import org.example.homedatazip.subscription.service.SubscriptionPhoneAuthService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<PhoneAuthVerifyResponse> verify(@Valid @RequestBody PhoneAuthVerifyRequest request) {
        return ResponseEntity.ok(
                phoneAuthService.verifyCode(request.phoneNumber(), request.requestId(), request.code())
        );
    }
}
