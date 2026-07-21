package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.config.ScdnIntegrationProperties;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.UserStatus;
import com.kuocai.cdn.service.SysUserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScdnSsoServiceTest {

    @Test
    void eligibilityRequiresRealNameForNormalUsers() {
        SysUserService users = mock(SysUserService.class);
        ScdnSsoService service = service(users);
        when(users.queryCacheUserById(7L)).thenReturn(user(7L, UserStatus.REGISTER_NOT_CERTIFIED.getCode(), 2L));

        ScdnContracts.UserEligibilityResponse response = service.eligibility(7L);

        assertFalse(response.isEligible());
        assertFalse(response.isRealNameVerified());
    }

    @Test
    void eligibilityRejectsUsersInTheIndependentBanTable() {
        SysUserService users = mock(SysUserService.class);
        ScdnStateEventReconciler stateEvents = mock(ScdnStateEventReconciler.class);
        ScdnSsoService service = service(users, mock(ScdnOneTimeCodeStore.class),
                mock(ScdnAccessTokenIssuer.class), stateEvents);
        when(users.queryCacheUserById(7L)).thenReturn(user(7L, UserStatus.CERTIFIED.getCode(), 2L));
        when(stateEvents.isBanned(7L)).thenReturn(true);

        ScdnContracts.UserEligibilityResponse response = service.eligibility(7L);

        assertFalse(response.isEligible());
        assertTrue(response.isRealNameVerified());
    }

    @Test
    void ssoCodeCanOnlyBeExchangedOnce() {
        SysUserService users = mock(SysUserService.class);
        ScdnOneTimeCodeStore codes = mock(ScdnOneTimeCodeStore.class);
        ScdnAccessTokenIssuer tokens = mock(ScdnAccessTokenIssuer.class);
        ScdnSsoService service = service(users, codes, tokens);
        when(users.queryCacheUserById(7L)).thenReturn(user(7L, UserStatus.CERTIFIED.getCode(), 2L));
        String code = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
        when(codes.take(anyString())).thenReturn("{\"userId\":7}").thenReturn(null);
        when(tokens.issue(any(), anyInt())).thenReturn("signed-token");

        ScdnContracts.SsoExchangeResponse first = service.exchange(code);
        assertEquals("signed-token", first.getAccessToken());
        assertTrue(first.getUser().isEligible());

        ScdnIntegrationException second = assertThrows(ScdnIntegrationException.class, () -> service.exchange(code));
        assertEquals("INVALID_SSO_CODE", second.getCode());
    }

    private ScdnSsoService service(SysUserService users) {
        return service(users, mock(ScdnOneTimeCodeStore.class), mock(ScdnAccessTokenIssuer.class),
                mock(ScdnStateEventReconciler.class));
    }

    private ScdnSsoService service(SysUserService users, ScdnOneTimeCodeStore codes, ScdnAccessTokenIssuer tokens) {
        return service(users, codes, tokens, mock(ScdnStateEventReconciler.class));
    }

    private ScdnSsoService service(SysUserService users, ScdnOneTimeCodeStore codes,
                                   ScdnAccessTokenIssuer tokens, ScdnStateEventReconciler stateEvents) {
        ScdnIntegrationProperties properties = new ScdnIntegrationProperties();
        properties.setEnabled(true);
        properties.setAccessTokenTtlSeconds(900);
        return new ScdnSsoService(properties, users, new ObjectMapper(), codes, tokens, stateEvents);
    }

    private SysUser user(Long id, String status, Long roleId) {
        return SysUser.builder().id(id).userName("test-user").email("user@example.com")
                .status(status).roleId(roleId).build();
    }
}
