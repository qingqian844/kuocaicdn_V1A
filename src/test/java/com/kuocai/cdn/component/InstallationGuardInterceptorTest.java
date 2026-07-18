package com.kuocai.cdn.component;

import com.kuocai.cdn.service.InstallationStateService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstallationGuardInterceptorTest {

    @Test
    void statusAndAdminLoginRemainAvailableWhilePending() throws Exception {
        InstallationGuardInterceptor interceptor = pendingInterceptor();

        assertTrue(interceptor.preHandle(request("/api/setup/status"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("/kuocaiadmin"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("/login/loginAdmin"), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void publicBusinessPagesAreBlockedWhilePending() throws Exception {
        InstallationGuardInterceptor interceptor = pendingInterceptor();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request("/register"), response, new Object());

        assertFalse(allowed);
        assertEquals("/kuocaiadmin", response.getRedirectedUrl());
    }

    @Test
    void ajaxBusinessCallsReceiveServiceUnavailable() throws Exception {
        InstallationGuardInterceptor interceptor = pendingInterceptor();
        MockHttpServletRequest request = request("/CdnDomain/create");
        request.addHeader("Accept", "application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains("首次初始化"));
    }

    private InstallationGuardInterceptor pendingInterceptor() {
        InstallationStateService service = mock(InstallationStateService.class);
        when(service.isPending()).thenReturn(true);
        return new InstallationGuardInterceptor(service);
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }
}
