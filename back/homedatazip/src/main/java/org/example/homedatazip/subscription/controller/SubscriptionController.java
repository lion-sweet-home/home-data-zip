package org.example.homedatazip.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.subscription.dto.SubscriptionStartResponse;
import org.example.homedatazip.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;


     // 구독 시작/재구독
    @PostMapping("/start")
    public ResponseEntity<SubscriptionStartResponse> startSubscription(
            @RequestBody StartSubscriptionRequest request
    ) {
        SubscriptionStartResponse response = subscriptionService.startSubscription(
                request.subscriberId(),
                request.name(),
                request.price(),
                request.periodDays()
        );
        return ResponseEntity.ok(response);
    }


     // 자동결제 OFF (다음 결제부터 중단)
    @PostMapping("/auto-pay/cancel")
    public ResponseEntity<Void> cancelAutoPay(@RequestParam Long subscriberId) {
        subscriptionService.cancelAutoPay(subscriberId);
        return ResponseEntity.noContent().build();
    }

    //자동결제 ON
    @PostMapping("/auto-pay/reactivate")
    public ResponseEntity<Void> reactivateAutoPay(@RequestParam Long subscriberId) {
        subscriptionService.reactivateAutoPay(subscriberId);
        return ResponseEntity.noContent().build();
    }


    // 내 구독 조회
    @GetMapping("/me")
    public ResponseEntity<SubscriptionStartResponse> getMySubscription(@RequestParam Long subscriberId) {
        return ResponseEntity.ok(subscriptionService.getMySubscription(subscriberId));
    }

    /**
     * Request DTO (startSubscription)
     */
    public record StartSubscriptionRequest(
            Long subscriberId,
            String name,
            Long price,
            int periodDays
    ) {}
}
