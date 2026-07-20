package com.kuocai.cdn.integration.scdn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

public final class ScdnContracts {
    private ScdnContracts() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Envelope<T> {
        private String code;
        private String message;
        private T data;

        public static <T> Envelope<T> success(T data) {
            return new Envelope<>("SUCCESS", "ok", data);
        }

        public static Envelope<Void> failure(String code, String message) {
            return new Envelope<>(code, message, null);
        }
    }

    @Data
    public static class SsoExchangeRequest {
        @NotBlank
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SsoExchangeResponse {
        private String accessToken;
        private String tokenType;
        private Integer expiresIn;
        private UserEligibilityResponse user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserEligibilityResponse {
        private Long userId;
        private String userName;
        private String email;
        private String role;
        private Long agentUserId;
        private String accountStatus;
        private boolean realNameVerified;
        private boolean eligible;
    }

    @Data
    public static class CreateOrderRequest {
        @NotBlank
        private String externalOrderId;
        @NotNull
        private Long userId;
        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
        @NotBlank
        private String title;
        private String detail;
        private String productCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderResponse {
        private String externalOrderId;
        private Long platformOrderId;
        private String platformOrderNumber;
        private Long userId;
        private BigDecimal amount;
        private String status;
    }

    @Data
    public static class WalletOperationRequest {
        @NotBlank
        private String businessReference;
        @NotNull
        private Long userId;
        @NotNull
        @Positive
        private BigDecimal amount;
        private String description;
        private String originalBusinessReference;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletOperationResponse {
        private String businessReference;
        private Long platformOrderId;
        private Long userId;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String operation;
        private String status;
    }
}

