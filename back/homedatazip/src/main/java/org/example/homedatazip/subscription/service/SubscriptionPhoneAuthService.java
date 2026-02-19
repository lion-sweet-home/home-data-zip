package org.example.homedatazip.subscription.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
import org.example.homedatazip.subscription.dto.PhoneAuthSendResponse;
import org.example.homedatazip.subscription.dto.PhoneAuthVerifyResponse;
import org.example.homedatazip.subscription.service.sender.SmsSender;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPhoneAuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(3);
    private static final Duration VERIFIED_TOKEN_TTL = Duration.ofMinutes(10);

    private final SmsSender smsSender;
    private final StringRedisTemplate redis;
    private final UserRepository userRepository;

    private final SecureRandom random = new SecureRandom();

    @PostConstruct
    void check() {
        log.info("[PHONE-AUTH] SmsSender impl = {}", smsSender.getClass().getName());
    }

    /**
     * 인증번호 발송
     */
    public PhoneAuthSendResponse sendCode(String rawPhoneNumber) {
        String phone = normalize(rawPhoneNumber);

        String requestId = UUID.randomUUID().toString();
        String code = generate6DigitCode();

        redis.opsForValue().set(codeKey(phone, requestId), code, CODE_TTL);

        smsSender.send(phone, "[HomeDataZip] 인증번호는 " + code + " 입니다. (유효시간 3분)");

        return new PhoneAuthSendResponse(requestId, (int) CODE_TTL.toSeconds());
    }

    /**
     * 인증번호 검증 + (성공 시) users.phone_number / users.phone_verified_at 업데이트
     */
    @Transactional
    public PhoneAuthVerifyResponse verifyCode(Long userId, String rawPhoneNumber, String requestId, String inputCode) {
        String phone = normalize(rawPhoneNumber);

        String key = codeKey(phone, requestId);
        String saved = redis.opsForValue().get(key);

        if (saved == null) {
            // 만료 or 없음
            return new PhoneAuthVerifyResponse(false, null);
        }

        if (!saved.equals(inputCode)) {
            // 불일치
            return new PhoneAuthVerifyResponse(false, null);
        }

        // 1회용 처리
        redis.delete(key);

        // DB 업데이트 (인증 성공 확정)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(SubscriptionErrorCode.SUBSCRIBER_NOT_FOUND));

        user.verifyPhone(phone); // phone_number + phone_verified_at=now()

        // (선택) 검증 완료 토큰 발급 - 프론트에서 다음 단계 이동 확인용
        String verificationToken = UUID.randomUUID().toString();
        redis.opsForValue().set(verifiedKey(phone, verificationToken), "OK", VERIFIED_TOKEN_TTL);

        return new PhoneAuthVerifyResponse(true, verificationToken);
    }

    /**
     * (선택) 토큰 기반 검증 체크 - 프론트에서 "방금 인증 완료했는지" 확인용
     */
    public boolean isVerified(String rawPhoneNumber, String verificationToken) {
        String phone = normalize(rawPhoneNumber);
        return redis.opsForValue().get(verifiedKey(phone, verificationToken)) != null;
    }

    private String generate6DigitCode() {
        int n = random.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private String normalize(String phoneNumber) {
        return phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
    }

    private String codeKey(String phone, String requestId) {
        return "phoneauth:code:" + phone + ":" + requestId;
    }

    private String verifiedKey(String phone, String token) {
        return "phoneauth:verified:" + phone + ":" + token;
    }
}
