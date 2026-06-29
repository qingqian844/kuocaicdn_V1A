package com.kuocai.cdn.dto.rest;

import lombok.Data;

@Data
public class SystemInfo {
    private String title;
    private String keywords;
    private String description;
    private String icon;
    private String logo;
    private String lightLogo;
    private String introduce;
    private Boolean isMaster;

    public SystemInfo() {
        this.title = "";
        this.keywords = "";
        this.description = "";
        this.icon = "";
        this.logo = "";
        this.lightLogo = "";
        this.introduce = "";
        this.isMaster = true;
    }
}
