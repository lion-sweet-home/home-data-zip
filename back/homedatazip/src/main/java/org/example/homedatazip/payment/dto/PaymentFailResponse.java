package org.example.homedatazip.payment.dto;

public record PaymentFailResponse(

        String orderId,
        String reason
) {}