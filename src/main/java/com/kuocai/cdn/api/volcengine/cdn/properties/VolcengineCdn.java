package com.kuocai.cdn.api.volcengine.cdn.properties;

import com.kuocai.cdn.util.RuntimeConfigUtils;

import java.util.TimeZone;

public class VolcengineCdn {

    public static final String TIME_FORMAT_V4 = "yyyyMMdd'T'HHmmss'Z'";
    public static final TimeZone tz = TimeZone.getTimeZone("UTC");
    public static final String Service = "CDN";
    public static final String Version = "2021-03-01";
    public static final String Region = "cn-north-1";
    public static final String Host = "cdn.volcengineapi.com";

    public static String AK = RuntimeConfigUtils.optional("volcengine.cdn.ak", "VOLCENGINE_CDN_AK", "");

    public static String SK = RuntimeConfigUtils.optional("volcengine.cdn.sk", "VOLCENGINE_CDN_SK", "");

    public static String Project = RuntimeConfigUtils.optional("volcengine.cdn.project", "VOLCENGINE_CDN_PROJECT", "default");

    public static String normalizeProject(String project) {
        if (project == null || project.trim().isEmpty()) {
            return "default";
        }
        String normalizedProject = project.trim();
        if ("kuocai".equalsIgnoreCase(normalizedProject)
                || "cdn".equalsIgnoreCase(normalizedProject)
                || "火山CDN".equals(normalizedProject)
                || "火山云".equals(normalizedProject)) {
            return "default";
        }
        return normalizedProject;
    }
}
