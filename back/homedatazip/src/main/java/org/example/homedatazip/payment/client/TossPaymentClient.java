package org.example.homedatazip.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.example.homedatazip.payment.client.dto.TossBillingKeyIssueRequest;
import org.example.homedatazip.payment.client.dto.TossBillingKeyIssueResponse;
import org.example.homedatazip.payment.dto.TossPaymentConfirmRequest;
import org.example.homedatazip.payment.dto.TossPaymentConfirmResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    @PostConstruct
    void checkKeys() {
        String masked = (secretKey == null) ? "null" :
                (secretKey.length() <= 8 ? secretKey : secretKey.substring(0, 8) + "...");
        log.info("[TOSS] secretKey prefix={}", masked);
    }


    @Value("${payment.toss.secret-key}")
    private String secretKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();

    public TossPaymentConfirmResponse approve(String paymentKey, String orderId, Long amount) {
        TossPaymentConfirmRequest payload = new TossPaymentConfirmRequest(paymentKey, orderId, amount);

        return webClient.post()
                .uri("/v1/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchangeToMono(resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    // ✅ 성공/실패 모두 body 로그 찍기
                                    if (resp.statusCode().is2xxSuccessful()) {
                                        log.info("[TOSS] approve success. body={}", body);
                                        try {
                                            ObjectMapper om = new ObjectMapper();
                                            return Mono.just(om.readValue(body, TossPaymentConfirmResponse.class));
                                        } catch (Exception e) {
                                            log.error("[TOSS] approve parse failed. body={}", body, e);
                                            return Mono.error(new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED));
                                        }
                                    }

                                    log.error("[TOSS] approve failed. status={}, body={}", resp.statusCode(), body);
                                    return Mono.error(new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED));
                                })
                )
                .block();
    }

    /**
     * 빌링키 발급 (카드등록 완료 후 authKey -> billingKey 교환)
     * POST /v1/billing/authorizations/issue
     */
    public TossBillingKeyIssueResponse issueBillingKey(String authKey, String customerKey) {
        return webClient.post()
                .uri("/v1/billing/authorizations/issue")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new TossBillingKeyIssueRequest(authKey, customerKey))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(
                                        new BusinessException(PaymentErrorCode.TOSS_BILLING_KEY_ISSUE_FAILED)
                                ))
                )
                .bodyToMono(TossBillingKeyIssueResponse.class)
                .block();
    }

    private String basicAuthHeader() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }

    private record TossConfirmRequest(
            String paymentKey,
            String orderId,
            Long amount
    ) {}

}
