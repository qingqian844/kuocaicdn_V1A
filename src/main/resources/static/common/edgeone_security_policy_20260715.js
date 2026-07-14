(function(window, $) {
    "use strict";

    function readPolicyState() {
        return {
            managedRulesEnabled: $("#eoManagedRulesEnabled").is(":checked") ? "on" : "off",
            managedRulesDetectionOnly: $("#eoManagedRulesDetectionOnly").is(":checked") ? "on" : "off",
            managedRulesSemanticAnalysis: $("#eoManagedRulesSemanticAnalysis").is(":checked") ? "on" : "off",
            managedRulesAutoUpdate: $("#eoManagedRulesAutoUpdate").is(":checked") ? "on" : "off",
            botManagementEnabled: $("#eoBotManagementEnabled").is(":checked") ? "on" : "off",
            captchaPageChallengeEnabled: $("#eoCaptchaPageChallengeEnabled").is(":checked") ? "on" : "off",
            aiCrawlerDetectionEnabled: $("#eoAiCrawlerDetectionEnabled").is(":checked") ? "on" : "off",
            aiCrawlerDetectionAction: $("#eoAiCrawlerDetectionAction").val(),
            httpDdosAdaptiveFrequencyControlEnabled: $("#eoHttpDdosAdaptiveFrequencyControlEnabled").is(":checked") ? "on" : "off",
            httpDdosAdaptiveFrequencyControlSensitivity: $("#eoHttpDdosAdaptiveFrequencyControlSensitivity").val(),
            httpDdosClientFilteringEnabled: $("#eoHttpDdosClientFilteringEnabled").is(":checked") ? "on" : "off",
            httpDdosBandwidthAbuseDefenseEnabled: $("#eoHttpDdosBandwidthAbuseDefenseEnabled").is(":checked") ? "on" : "off",
            httpDdosSlowAttackDefenseEnabled: $("#eoHttpDdosSlowAttackDefenseEnabled").is(":checked") ? "on" : "off",
            rateLimitEnabled: $("#eoRateLimitEnabled").is(":checked") ? "on" : "off",
            rateLimitThreshold: Number($("#eoRateLimitThreshold").val() || 0) || 1000,
            rateLimitPeriod: $("#eoRateLimitPeriod").val(),
            rateLimitMode: $("#eoRateLimitMode").val(),
            rateLimitAction: $("#eoRateLimitAction").val(),
            rateLimitChallengeOption: $("#eoRateLimitChallengeOption").val(),
            rateLimitActionDuration: $("#eoRateLimitActionDuration").val(),
            rateLimitCountBy: $("#eoRateLimitCountBy").val(),
            rateLimitCondition: $("#eoRateLimitCondition").val(),
            exceptionEnabled: $("#eoExceptionEnabled").is(":checked") ? "on" : "off",
            exceptionModules: $("#eoExceptionModules").val(),
            exceptionCondition: $("#eoExceptionCondition").val()
        };
    }

    function detectChangedModules(initialState, currentState) {
        const moduleFields = {
            "managed-rules": ["managedRulesEnabled", "managedRulesDetectionOnly", "managedRulesSemanticAnalysis", "managedRulesAutoUpdate"],
            "bot-management": ["botManagementEnabled"],
            "bot-management-lite": ["captchaPageChallengeEnabled", "aiCrawlerDetectionEnabled", "aiCrawlerDetectionAction"],
            "http-ddos-protection": ["httpDdosAdaptiveFrequencyControlEnabled", "httpDdosAdaptiveFrequencyControlSensitivity", "httpDdosClientFilteringEnabled", "httpDdosBandwidthAbuseDefenseEnabled", "httpDdosSlowAttackDefenseEnabled"],
            "rate-limiting-rules": ["rateLimitEnabled", "rateLimitThreshold", "rateLimitPeriod", "rateLimitMode", "rateLimitAction", "rateLimitChallengeOption", "rateLimitActionDuration", "rateLimitCountBy", "rateLimitCondition"],
            "exception-rules": ["exceptionEnabled", "exceptionModules", "exceptionCondition"]
        };
        return Object.keys(moduleFields).filter(function(moduleName) {
            return moduleFields[moduleName].some(function(fieldName) {
                return JSON.stringify(initialState[fieldName]) !== JSON.stringify(currentState[fieldName]);
            });
        });
    }

    function captureInitialState() {
        if (document.getElementById("edgeOneSecurityPolicyButton")) {
            window.edgeOneSecurityInitialState = readPolicyState();
        }
    }

    window.readEdgeOneSecurityPolicyState = readPolicyState;
    window.detectChangedEdgeOneSecurityModules = detectChangedModules;
    window.saveEdgeOneSecurityPolicy = async function(id) {
        const rawThreshold = Number($("#eoRateLimitThreshold").val() || 0);
        const currentState = readPolicyState();
        if (currentState.rateLimitEnabled === "on"
                && (!Number.isFinite(rawThreshold) || rawThreshold < 1 || rawThreshold > 100000)) {
            layerWarn("速率限制阈值必须在 1 - 100000 之间");
            errorShake("eoRateLimitThreshold");
            return;
        }
        if (currentState.exceptionEnabled === "on"
                && !String(currentState.exceptionCondition || "").trim()) {
            layerWarn("启用例外规则时必须填写匹配条件");
            errorShake("eoExceptionCondition");
            return;
        }

        const param = Object.assign({doMainId: id}, currentState);
        if (window.edgeOneSecurityInitialState) {
            param.changedModules = detectChangedModules(window.edgeOneSecurityInitialState, currentState);
        }
        const data = await sendRequest(
            "POST",
            "CdnDomainAccess/saveEdgeOneSecurityPolicy",
            JSON.stringify(param),
            "application/json"
        );
        autoLayer(data);
        if (data["code"] === "SUCCESS") {
            setTimeout(function() {
                reload();
            }, 1000);
        }
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", captureInitialState, {once: true});
    } else {
        captureInitialState();
    }
})(window, window.jQuery);
