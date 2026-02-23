package org.example.homedatazip.subscription.service.sender;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.SubscriptionErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
public class SolapiSmsSender implements SmsSender {

    @Value("${sms.solapi.api-key}")
    private String apiKey;

    @Value("${sms.solapi.api-secret}")
    private String apiSecret;

    @Value("${sms.solapi.from}")
    private String from;

    private DefaultMessageService messageService;

    @PostConstruct
    void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
        log.info("[SOLAPI] initialized");
    }

    @Override
    public void send(String phoneNumber, String messageText) {
        String to = phoneNumber.replaceAll("\\D", "");

        Message message = new Message();
        message.setFrom(from);
        message.setTo(to);
        message.setText(messageText);

        try {
            messageService.send(message);
            log.info("[SOLAPI] SMS sent to={}", to);
        } catch (Exception e) {
            log.error("[SOLAPI] SMS send failed to={}, err={}", to, e.getMessage(), e);
            throw new BusinessException(SubscriptionErrorCode.SMS_SEND_FAILED);
        }
    }
}
