package org.example.homedatazip.payment.client;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.PaymentErrorCode;
import org.example.homedatazip.payment.dto.TossPaymentConfirmResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class TossPaymentClient {

    @Value("${payment.toss.secret-key}")
    private String secretKey;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.tosspayments.com")
            .build();

    public TossPaymentConfirmResponse approve(String paymentKey, String orderId, Long amount) {

        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        try {
            return webClient.post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new TossConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            resp -> resp.bodyToMono(String.class)
                                    .map(body -> new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED))
                    )
                    .bodyToMono(TossPaymentConfirmResponse.class)
                    .block();

        } catch (Exception e) {
            throw new BusinessException(PaymentErrorCode.TOSS_APPROVE_FAILED);
        }
    }

    private record TossConfirmRequest(
            String paymentKey,
            String orderId,
            Long amount
    ) {}
}
