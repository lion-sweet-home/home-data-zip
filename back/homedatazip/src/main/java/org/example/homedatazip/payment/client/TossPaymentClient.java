package org.example.homedatazip.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.example.homedatazip.payment.client.dto.TossBillingKeyIssueRequest;
import org.example.homedatazip.payment.client.dto.TossBillingKeyIssueResponse;
import org.example.homedatazip.payment.client.dto.TossBillingPaymentRequest;
import org.example.homedatazip.payment.client.dto.TossBillingPaymentResponse;
import org.example.homedatazip.payment.dto.TossPaymentConfirmRequest;
import org.example.homedatazip.payment.dto.TossPaymentConfirmResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    @Value("${payment.toss.secret-key}")
    private String secretKey;

    private final ObjectMapper objectMapper;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();

    @PostConstruct
    void init() {
        String masked = (secretKey == null) ? "null"
                : (secretKey.length() <= 8 ? secretKey : secretKey.substring(0, 8) + "...");
        log.info("[TOSS] secretKey prefix={}", masked);

        objectMapper.registerModule(new JavaTimeModule());
    }

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
                                    if (resp.statusCode().is2xxSuccessful()) {
                                        log.info("[TOSS] approve success. body={}", body);
                                        try {
                                            return Mono.just(objectMapper.readValue(body, TossPaymentConfirmResponse.class));
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
     * 정기결제/첫결제(빌링키 결제)
     * POST /v1/billing/{billingKey}
     */
    public TossBillingPaymentResponse payWithBillingKey(
            String billingKey,
            String customerKey,
            String orderId,
            String orderName,
            Long amount
    ) {
        TossBillingPaymentRequest payload =
                new TossBillingPaymentRequest(customerKey, amount, orderId, orderName);

        return webClient.post()
                .uri("/v1/billing/{billingKey}", billingKey)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchangeToMono(resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    if (resp.statusCode().is2xxSuccessful()) {
                                        log.info("[TOSS] billing pay success. orderId={}, body={}", orderId, body);
                                        try {
                                            return Mono.just(objectMapper.readValue(body, TossBillingPaymentResponse.class));
                                        } catch (Exception e) {
                                            log.error("[TOSS] billing pay parse failed. orderId={}, body={}", orderId, body, e);
                                            return Mono.error(new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED));
                                        }
                                    }

                                    log.error("[TOSS] billing pay failed. orderId={}, status={}, body={}",
                                            orderId, resp.statusCode(), body);
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
                                .flatMap(body -> {
                                    log.error("[TOSS] billing key issue failed. status={}, body={}", resp.statusCode(), body);
                                    return Mono.error(new BusinessException(PaymentErrorCode.TOSS_BILLING_KEY_ISSUE_FAILED));
                                })
                )
                .bodyToMono(TossBillingKeyIssueResponse.class)
                .block();
    }

    private String basicAuthHeader() {
        return "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}
