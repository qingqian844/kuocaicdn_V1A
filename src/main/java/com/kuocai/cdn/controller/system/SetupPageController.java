package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.service.InstallationStateService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SetupPageController {

    private final InstallationStateService installationStateService;

    public SetupPageController(InstallationStateService installationStateService) {
        this.installationStateService = installationStateService;
    }

    @GetMapping("/setup")
    public String setup() {
        return installationStateService.isPending() ? "admin/setup" : "redirect:/dashboard";
    }
}
