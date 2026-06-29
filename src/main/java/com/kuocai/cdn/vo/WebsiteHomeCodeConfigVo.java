package com.kuocai.cdn.vo;

/**
 * Home page custom frontend code.
 */
public class WebsiteHomeCodeConfigVo {

    /**
     * Enable custom home page rendering.
     */
    private Boolean enabled;

    /**
     * HTML rendered inside the home page main content area.
     */
    private String htmlCode;

    /**
     * CSS injected into the home page head.
     */
    private String cssCode;

    /**
     * JavaScript injected at the bottom of the home page.
     */
    private String jsCode;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getHtmlCode() {
        return htmlCode;
    }

    public void setHtmlCode(String htmlCode) {
        this.htmlCode = htmlCode;
    }

    public String getCssCode() {
        return cssCode;
    }

    public void setCssCode(String cssCode) {
        this.cssCode = cssCode;
    }

    public String getJsCode() {
        return jsCode;
    }

    public void setJsCode(String jsCode) {
        this.jsCode = jsCode;
    }
}
