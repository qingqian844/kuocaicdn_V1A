package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.config.ScdnIntegrationProperties;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.UserStatus;
import com.kuocai.cdn.service.SysUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class ScdnSsoService {
    private static final String CODE_PREFIX = "scdn:sso:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ScdnIntegrationProperties properties;
    private final SysUserService sysUserService;
    private final ObjectMapper objectMapper;
    private final ScdnOneTimeCodeStore codeStore;
    private final ScdnAccessTokenIssuer tokenIssuer;
    private final ScdnStateEventReconciler stateEvents;

    public ScdnSsoService(ScdnIntegrationProperties properties,
                          SysUserService sysUserService,
                          ObjectMapper objectMapper,
                          ScdnOneTimeCodeStore codeStore,
                          ScdnAccessTokenIssuer tokenIssuer,
                          ScdnStateEventReconciler stateEvents) {
        this.properties = properties;
        this.sysUserService = sysUserService;
        this.objectMapper = objectMapper;
        this.codeStore = codeStore;
        this.tokenIssuer = tokenIssuer;
        this.stateEvents = stateEvents;
    }

    public String issue(SysUser user) {
        if (!properties.isEnabled()) {
            throw new ScdnIntegrationException("INTEGRATION_DISABLED", "SCDN integration is disabled", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (user == null || user.getId() == null) {
            throw new ScdnIntegrationException("UNAUTHENTICATED", "Login is required", HttpStatus.UNAUTHORIZED);
        }
        try {
            byte[] random = new byte[32];
            RANDOM.nextBytes(random);
            String code = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            Map<String, Object> value = new HashMap<>();
            value.put("userId", user.getId());
            value.put("issuedAt", System.currentTimeMillis());
            codeStore.put(CODE_PREFIX + sha256(code), objectMapper.writeValueAsString(value),
                    properties.getSsoCodeTtlSeconds());
            return code;
        } catch (Exception e) {
            throw new ScdnIntegrationException("SSO_CODE_FAILED", "Unable to issue SCDN login code", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public ScdnContracts.SsoExchangeResponse exchange(String code) {
        if (code == null || code.length() < 32 || code.length() > 128) {
            throw invalidCode();
        }
        try {
            String raw = codeStore.take(CODE_PREFIX + sha256(code));
            if (raw == null) {
                throw invalidCode();
            }
            Map<String, Object> stored = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            Number userId = (Number) stored.get("userId");
            SysUser user = userId == null ? null : sysUserService.queryCacheUserById(userId.longValue());
            if (user == null) {
                throw invalidCode();
            }
            ScdnContracts.UserEligibilityResponse eligibility = eligibility(user);
            if (!eligibility.isEligible()) {
                throw new ScdnIntegrationException("USER_NOT_ELIGIBLE", "The account is not eligible for SCDN", HttpStatus.FORBIDDEN);
            }
            Map<String, String> claims = new HashMap<>();
            claims.put("sub", user.getId().toString());
            claims.put("userId", user.getId().toString());
            claims.put("userName", safe(user.getUserName()));
            claims.put("role", eligibility.getRole());
            claims.put("kyc", Boolean.toString(eligibility.isRealNameVerified()));
            if (user.getAgentUserId() != null) {
                claims.put("agentUserId", user.getAgentUserId().toString());
            }
            String token = tokenIssuer.issue(claims, properties.getAccessTokenTtlSeconds());
            return ScdnContracts.SsoExchangeResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(properties.getAccessTokenTtlSeconds())
                    .user(eligibility)
                    .build();
        } catch (ScdnIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw invalidCode();
        }
    }

    public ScdnContracts.UserEligibilityResponse eligibility(Long userId) {
        SysUser user = userId == null ? null : sysUserService.queryCacheUserById(userId);
        if (user == null) {
            throw new ScdnIntegrationException("USER_NOT_FOUND", "User does not exist", HttpStatus.NOT_FOUND);
        }
        return eligibility(user);
    }

    private ScdnContracts.UserEligibilityResponse eligibility(SysUser user) {
        boolean admin = user.getRoleId() != null && user.getRoleId() == 1L;
        boolean verified = UserStatus.CERTIFIED.getCode().equals(user.getStatus()) || admin;
        boolean banned = stateEvents.isBanned(user.getId());
        boolean active = !banned && !UserStatus.BANNED.getCode().equals(user.getStatus())
                && !UserStatus.CANCELLATION.getCode().equals(user.getStatus());
        ScdnContracts.UserEligibilityResponse response = ScdnContracts.UserEligibilityResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(admin ? "ADMIN" : "USER")
                .agentUserId(user.getAgentUserId())
                .accountStatus(user.getStatus())
                .realNameVerified(verified)
                .eligible(active && verified)
                .build();
        stateEvents.trackUser(response, banned);
        return response;
    }

    private ScdnIntegrationException invalidCode() {
        return new ScdnIntegrationException("INVALID_SSO_CODE", "SSO code is invalid, expired, or already used", HttpStatus.UNAUTHORIZED);
    }

    private String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte item : digest) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
