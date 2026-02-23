package org.example.homedatazip.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.email.entity.EmailAuth;
import org.example.homedatazip.email.repository.EmailAuthRedisRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.EmailErrorCode;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final EmailAuthRedisRepository emailAuthRepository;
    private final JavaMailSender mailSender;

    // 1. 인증 코드 발송 및 Redis 저장
    public void sendAuthCode(String email) {
        String authCode = UUID.randomUUID().toString().substring(0, 6);

        // Redis에 저장 (이미 존재하면 덮어쓰기 및 TTL 초기화)
        EmailAuth auth = EmailAuth.create(email, authCode);

        emailAuthRepository.save(auth);
        sendEmail(email, authCode);
    }

    // 2. 인증 코드 검증
    public EmailAuth verifyCode(String email, String code) {
        // Redis에서 조회, 없으면 만료된 것으로 간주
        EmailAuth auth = emailAuthRepository.findById(email)
                .orElseThrow(() -> new BusinessException(EmailErrorCode.AUTH_EXPIRED_OR_NOT_FOUND));

        if (!auth.getAuthCode().equals(code)) {
            throw new BusinessException(EmailErrorCode.INVALID_AUTH_CODE);
        }

        // 인증 완료 처리 (재저장을 통해 상태 업데이트)
        auth.updateVerified(true);
        emailAuthRepository.save(auth);

        return auth;
    }

    private void sendEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[Homedatazip] 인증 코드");
        message.setText("인증 코드: " + code);
        log.info("인증 코드: {}", code);
        mailSender.send(message);
    }
}
