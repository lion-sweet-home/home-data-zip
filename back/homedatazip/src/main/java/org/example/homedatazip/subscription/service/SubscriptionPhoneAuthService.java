package org.example.homedatazip.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.subscription.dto.PhoneAuthSendResponse;
import org.example.homedatazip.subscription.dto.PhoneAuthVerifyResponse;
import org.example.homedatazip.subscription.service.sender.SmsSender;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionPhoneAuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(3);
    private static final Duration VERIFIED_TOKEN_TTL = Duration.ofMinutes(10);

    private final SmsSender smsSender;
    private final StringRedisTemplate redis;

    private final SecureRandom random = new SecureRandom();

    public PhoneAuthSendResponse sendCode(String rawPhoneNumber) {
        String phone = normalize(rawPhoneNumber);

        String requestId = UUID.randomUUID().toString();
        String code = generate6DigitCode();

        redis.opsForValue().set(codeKey(phone, requestId), code, CODE_TTL);

        smsSender.send(phone, "[HomeDataZip] 인증번호는 " + code + " 입니다. (유효시간 3분)");

        return new PhoneAuthSendResponse(requestId, (int) CODE_TTL.toSeconds());
    }

    public PhoneAuthVerifyResponse verifyCode(String rawPhoneNumber, String requestId, String inputCode) {
        String phone = normalize(rawPhoneNumber);

        String key = codeKey(phone, requestId);
        String saved = redis.opsForValue().get(key);

        if (saved == null) { // 만료 or 없음
            return new PhoneAuthVerifyResponse(false, null);
        }
        if (!saved.equals(inputCode)) { // 불일치
            return new PhoneAuthVerifyResponse(false, null);
        }

        // 1회용 처리
        redis.delete(key);

        // 검증 완료 토큰 발급 (다음 단계에서 사용 가능)
        String verificationToken = UUID.randomUUID().toString();
        redis.opsForValue().set(verifiedKey(phone, verificationToken), "OK", VERIFIED_TOKEN_TTL);

        return new PhoneAuthVerifyResponse(true, verificationToken);
    }

    public boolean isVerified(String rawPhoneNumber, String verificationToken) {
        String phone = normalize(rawPhoneNumber);
        return redis.opsForValue().get(verifiedKey(phone, verificationToken)) != null;
    }

    private String generate6DigitCode() {
        int n = random.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private String normalize(String phoneNumber) {
        return phoneNumber.replaceAll("\\D", "");
    }

    private String codeKey(String phone, String requestId) {
        return "phoneauth:code:" + phone + ":" + requestId;
    }

    private String verifiedKey(String phone, String token) {
        return "phoneauth:verified:" + phone + ":" + token;
    }
}
