package com.kuocai.cdn.controller.base;

import com.kuocai.cdn.entity.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@CrossOrigin
public class ApiV1Controller {

    protected boolean isAgent = false;
    protected AgentConfig agentConfig;

    @ModelAttribute
    public void init(HttpServletRequest request, HttpServletResponse response) {
        isAgent = false;
        agentConfig = null;
    }
}
