package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallationStateVo {

    public static final String PENDING = "PENDING";
    public static final String COMPLETED = "COMPLETED";

    private String status;
    private Integer currentStep;
    private Boolean bootstrapPasswordApplied;
    private Boolean adminConfigured;
    private Boolean domainVerified;
    private Boolean proxyConfigured;
    private Boolean websiteConfigured;
    private String domain;
    private String completedAt;
    private Map<String, Boolean> modules;
    private Map<String, String> moduleTestHashes;

    public static InstallationStateVo pending() {
        return InstallationStateVo.builder()
                .status(PENDING)
                .currentStep(1)
                .bootstrapPasswordApplied(false)
                .adminConfigured(false)
                .domainVerified(false)
                .proxyConfigured(false)
                .websiteConfigured(false)
                .modules(new LinkedHashMap<>())
                .moduleTestHashes(new LinkedHashMap<>())
                .build();
    }

    public static InstallationStateVo completed() {
        return InstallationStateVo.builder()
                .status(COMPLETED)
                .currentStep(8)
                .bootstrapPasswordApplied(true)
                .adminConfigured(true)
                .domainVerified(true)
                .proxyConfigured(true)
                .websiteConfigured(true)
                .modules(new LinkedHashMap<>())
                .moduleTestHashes(new LinkedHashMap<>())
                .build();
    }

    public void normalize() {
        if (status == null) {
            status = PENDING;
        }
        if (currentStep == null || currentStep < 1) {
            currentStep = 1;
        }
        if (modules == null) {
            modules = new LinkedHashMap<>();
        }
        if (moduleTestHashes == null) {
            moduleTestHashes = new LinkedHashMap<>();
        }
    }
}
