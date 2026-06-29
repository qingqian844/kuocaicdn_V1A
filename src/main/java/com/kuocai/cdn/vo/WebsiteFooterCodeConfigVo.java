package com.kuocai.cdn.vo;

/**
 * Footer custom frontend code.
 */
public class WebsiteFooterCodeConfigVo {

    /**
     * Enable custom footer rendering.
     */
    private Boolean enabled;

    /**
     * HTML rendered in place of the default footer.
     */
    private String htmlCode;

    /**
     * CSS injected before the custom footer.
     */
    private String cssCode;

    /**
     * JavaScript injected after the custom footer.
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
