package com.kuocai.cdn.integration.scdn;

import com.kuocai.cdn.config.ScdnIntegrationProperties;
import com.kuocai.cdn.controller.base.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ScdnEntryController extends BaseController {
    private final ScdnIntegrationProperties properties;
    private final ScdnSsoService ssoService;

    public ScdnEntryController(ScdnIntegrationProperties properties, ScdnSsoService ssoService) {
        this.properties = properties;
        this.ssoService = ssoService;
    }

    @GetMapping("/scdn")
    public void enter(HttpServletResponse response) throws IOException {
        String code = ssoService.issue(loginUser);
        String target = UriComponentsBuilder.fromHttpUrl(properties.getConsoleUrl())
                .path("/sso/callback")
                .queryParam("code", code)
                .build(true)
                .toUriString();
        response.sendRedirect(target);
    }
}

