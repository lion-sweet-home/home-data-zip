package org.example.homedatazip.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossBillingKeyIssueResponse(
        String mId,
        String customerKey,
        String billingKey
) {}
