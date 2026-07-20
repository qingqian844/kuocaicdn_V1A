package com.kuocai.cdn.integration.scdn;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/internal/scdn/v1")
public class ScdnInternalController {
    private final ScdnSsoService ssoService;
    private final ScdnPlatformBillingService billingService;

    public ScdnInternalController(ScdnSsoService ssoService, ScdnPlatformBillingService billingService) {
        this.ssoService = ssoService;
        this.billingService = billingService;
    }

    @PostMapping("/sso/exchange")
    public ScdnContracts.Envelope<ScdnContracts.SsoExchangeResponse> exchange(
            @Valid @RequestBody ScdnContracts.SsoExchangeRequest request) {
        return ScdnContracts.Envelope.success(ssoService.exchange(request.getCode()));
    }

    @GetMapping("/users/{userId}/eligibility")
    public ScdnContracts.Envelope<ScdnContracts.UserEligibilityResponse> eligibility(@PathVariable Long userId) {
        return ScdnContracts.Envelope.success(ssoService.eligibility(userId));
    }

    @PostMapping("/orders")
    public ScdnContracts.Envelope<ScdnContracts.OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ScdnContracts.CreateOrderRequest request) {
        return ScdnContracts.Envelope.success(billingService.createOrder(idempotencyKey, request));
    }

    @GetMapping("/orders/{externalOrderId}")
    public ScdnContracts.Envelope<ScdnContracts.OrderResponse> getOrder(@PathVariable String externalOrderId) {
        return ScdnContracts.Envelope.success(billingService.getOrder(externalOrderId));
    }

    @PostMapping("/wallet/debits")
    public ScdnContracts.Envelope<ScdnContracts.WalletOperationResponse> debit(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ScdnContracts.WalletOperationRequest request) {
        return ScdnContracts.Envelope.success(billingService.debit(idempotencyKey, request));
    }

    @PostMapping("/wallet/refunds")
    public ScdnContracts.Envelope<ScdnContracts.WalletOperationResponse> refund(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ScdnContracts.WalletOperationRequest request) {
        return ScdnContracts.Envelope.success(billingService.refund(idempotencyKey, request));
    }
}

