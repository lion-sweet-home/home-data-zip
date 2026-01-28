package org.example.homedatazip.payment.dto;

import java.util.List;

//결제로그 리스트 응답
public record PaymentListResponse(
        Long subscriptionId,
        List<PaymentLogResponse> payments
) {}