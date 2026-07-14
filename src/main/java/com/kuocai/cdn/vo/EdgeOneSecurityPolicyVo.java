package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EdgeOneSecurityPolicyVo {

    private String doMainId;

    private List<String> changedModules;

    private String managedRulesEnabled;

    private String managedRulesDetectionOnly;

    private String managedRulesSemanticAnalysis;

    private String managedRulesAutoUpdate;

    private String botManagementEnabled;

    private String captchaPageChallengeEnabled;

    private String aiCrawlerDetectionEnabled;

    private String aiCrawlerDetectionAction;

    private String httpDdosAdaptiveFrequencyControlEnabled;

    private String httpDdosAdaptiveFrequencyControlSensitivity;

    private String httpDdosClientFilteringEnabled;

    private String httpDdosBandwidthAbuseDefenseEnabled;

    private String httpDdosSlowAttackDefenseEnabled;

    private String rateLimitEnabled;

    private String rateLimitCondition;

    private String rateLimitCountBy;

    private Long rateLimitThreshold;

    private String rateLimitPeriod;

    private String rateLimitMode;

    private String rateLimitActionDuration;

    private String rateLimitAction;

    private String rateLimitChallengeOption;

    private String exceptionEnabled;

    private String exceptionCondition;

    private String exceptionModules;
}
