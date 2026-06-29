package com.kuocai.cdn.controller.base;

import com.kuocai.cdn.entity.AgentConfig;
import com.kuocai.cdn.service.AgentConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@CrossOrigin
public class ApiV1Controller {

    protected boolean isAgent = false;
    protected AgentConfig agentConfig;

    @Autowired
    protected AgentConfigService agentConfigService;

    @ModelAttribute
    public void init(HttpServletRequest request, HttpServletResponse response) {
        String host = request.getHeader("Host");
        if (host != null && !host.contains("kuocaicdn.com")) {
            agentConfig = agentConfigService.queryByDomain(host);
            if (agentConfig != null) {
                isAgent = true;
            }
        }
    }
}
