package org.example.homedatazip.subscription.service.sender;

public interface SmsSender {
    void send(String phoneNumber, String message);
}
