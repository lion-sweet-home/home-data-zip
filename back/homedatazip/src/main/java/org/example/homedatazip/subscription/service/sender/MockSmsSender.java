package org.example.homedatazip.subscription.service.sender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"!local"})
public class MockSmsSender implements SmsSender {
    @Override
    public void send(String phoneNumber, String message) {
        log.info("[MOCK-SMS] to={}, msg={}", phoneNumber, message);
    }
}
