package com.kuocai.cdn.util;

import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import com.kuocai.cdn.vo.WebsiteHomeCodeConfigVo;

/**
 * Editable default home page code shown in the admin editor.
 */
public class WebsiteHomeCodeDefaults {

    private WebsiteHomeCodeDefaults() {
    }

    public static WebsiteHomeCodeConfigVo build(WebsiteBaseConfigVo config) {
        WebsiteHomeCodeConfigVo vo = new WebsiteHomeCodeConfigVo();
        vo.setEnabled(false);
        vo.setHtmlCode(defaultHtml(config));
        vo.setCssCode(defaultCss());
        vo.setJsCode(defaultJs());
        return vo;
    }

    private static String defaultHtml(WebsiteBaseConfigVo config) {
        String name = text(config == null ? null : config.getWebsiteName(), "括彩云");
        String intro = text(config == null ? null : config.getWebsiteAnnouncement(), "企业级智能 CDN 服务平台，提供国内外内容分发、加速、防护与智能调度服务。");
        String inviteReward = number(config == null ? null : config.getInviteRewardGb(), "100");
        String invitedReward = number(config == null ? null : config.getInvitedRewardGb(), "100");
        String monthGift = number(config == null ? null : config.getMonthGiftGb(), "100");

        return ""
                + "<section class=\"home-hero\">\n"
                + "  <div class=\"container\">\n"
                + "    <div class=\"home-hero-inner\">\n"
                + "      <span class=\"home-eyebrow\">企业级智能 CDN 服务平台</span>\n"
                + "      <h1>" + escape(name) + "</h1>\n"
                + "      <p>" + escape(intro) + "</p>\n"
                + "      <div class=\"home-actions\">\n"
                + "        <a class=\"btn btn-light btn-lg\" href=\"/dashboard\">立即使用</a>\n"
                + "        <a class=\"btn btn-outline-light btn-lg\" href=\"#Pricing\">查看套餐</a>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "</section>\n\n"
                + "<section class=\"home-rewards\">\n"
                + "  <div class=\"container\">\n"
                + "    <div class=\"row g-3\">\n"
                + "      <div class=\"col-md-4\"><div class=\"home-stat\"><strong>" + escape(inviteReward) + "G</strong><span>邀请注册奖励</span></div></div>\n"
                + "      <div class=\"col-md-4\"><div class=\"home-stat\"><strong>" + escape(invitedReward) + "G</strong><span>被邀请人奖励</span></div></div>\n"
                + "      <div class=\"col-md-4\"><div class=\"home-stat\"><strong>" + escape(monthGift) + "G</strong><span>实名认证每月赠送</span></div></div>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "</section>\n\n"
                + "<section class=\"home-section\">\n"
                + "  <div class=\"container\">\n"
                + "    <div class=\"home-section-title\">\n"
                + "      <h2>核心功能及服务</h2>\n"
                + "      <p>覆盖加速、防护、调度、监控等高频业务场景。</p>\n"
                + "    </div>\n"
                + "    <div class=\"row g-4\">\n"
                + "      <div class=\"col-md-6 col-lg-3\"><div class=\"home-card\"><h3>全球节点覆盖</h3><p>精选主流厂商节点，覆盖各地区与运营商网络。</p></div></div>\n"
                + "      <div class=\"col-md-6 col-lg-3\"><div class=\"home-card\"><h3>智能调度</h3><p>支持 DNS、HTTPDNS、HTTP302 等调度方式。</p></div></div>\n"
                + "      <div class=\"col-md-6 col-lg-3\"><div class=\"home-card\"><h3>安全防护</h3><p>支持高防、访问控制、黑白名单与防盗链能力。</p></div></div>\n"
                + "      <div class=\"col-md-6 col-lg-3\"><div class=\"home-card\"><h3>数据统计</h3><p>提供流量、带宽、状态码等多维度业务统计。</p></div></div>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "</section>\n\n"
                + "<section id=\"Pricing\" class=\"home-pricing\">\n"
                + "  <div class=\"container\">\n"
                + "    <div class=\"home-pricing-box\">\n"
                + "      <h2>灵活套餐，按需购买</h2>\n"
                + "      <p>可以在后台产品管理维护套餐，也可以直接修改这里的展示文案和按钮链接。</p>\n"
                + "      <a class=\"btn btn-primary btn-lg\" href=\"/hot\">查看热门套餐</a>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "</section>\n\n"
                + "<section class=\"home-section home-final\">\n"
                + "  <div class=\"container text-center\">\n"
                + "    <h2>开始使用 " + escape(name) + "</h2>\n"
                + "    <p>快速接入 CDN 加速服务，统一管理域名、证书、缓存和数据统计。</p>\n"
                + "    <a class=\"btn btn-primary btn-lg\" href=\"/dashboard\">进入控制台</a>\n"
                + "  </div>\n"
                + "</section>\n";
    }

    private static String defaultCss() {
        return ""
                + ".home-hero { background: linear-gradient(135deg, #156ff7 0%, #00b8d9 100%); color: #fff; }\n"
                + ".home-hero-inner { max-width: 820px; margin: 0 auto; padding: 96px 0 112px; text-align: center; }\n"
                + ".home-eyebrow { display: inline-block; margin-bottom: 18px; font-weight: 700; opacity: .9; }\n"
                + ".home-hero h1 { color: #fff; font-size: 52px; font-weight: 800; margin-bottom: 22px; }\n"
                + ".home-hero p { font-size: 20px; line-height: 1.8; opacity: .94; margin-bottom: 32px; }\n"
                + ".home-actions { display: flex; gap: 14px; justify-content: center; flex-wrap: wrap; }\n"
                + ".home-rewards { margin-top: -42px; position: relative; z-index: 2; }\n"
                + ".home-stat { height: 128px; border-radius: 14px; background: #fff; box-shadow: 0 14px 34px rgba(19,33,68,.12); display: flex; flex-direction: column; align-items: center; justify-content: center; }\n"
                + ".home-stat strong { color: #156ff7; font-size: 34px; line-height: 1; margin-bottom: 10px; }\n"
                + ".home-stat span { color: #5f6b7a; font-weight: 600; }\n"
                + ".home-section { padding: 84px 0; }\n"
                + ".home-section-title { max-width: 680px; margin: 0 auto 36px; text-align: center; }\n"
                + ".home-section-title h2, .home-pricing-box h2, .home-final h2 { color: #132144; font-size: 34px; font-weight: 800; }\n"
                + ".home-section-title p, .home-pricing-box p, .home-final p { color: #677788; font-size: 17px; line-height: 1.8; }\n"
                + ".home-card { height: 100%; min-height: 190px; border: 1px solid #e7eaf3; border-radius: 12px; padding: 28px; background: #fff; box-shadow: 0 8px 24px rgba(19,33,68,.05); }\n"
                + ".home-card h3 { color: #132144; font-size: 20px; font-weight: 800; margin-bottom: 12px; }\n"
                + ".home-card p { color: #677788; line-height: 1.7; margin: 0; }\n"
                + ".home-pricing { padding: 80px 0; background: #f7faff; }\n"
                + ".home-pricing-box { text-align: center; max-width: 760px; margin: 0 auto; }\n"
                + ".home-final { background: #fff; }\n"
                + "@media (max-width: 768px) { .home-hero-inner { padding: 72px 0 92px; } .home-hero h1 { font-size: 36px; } .home-hero p { font-size: 17px; } }\n";
    }

    private static String defaultJs() {
        return ""
                + "document.querySelectorAll('a[href^=\"#\"]').forEach(function (link) {\n"
                + "  link.addEventListener('click', function (event) {\n"
                + "    var target = document.querySelector(link.getAttribute('href'));\n"
                + "    if (target) {\n"
                + "      event.preventDefault();\n"
                + "      target.scrollIntoView({ behavior: 'smooth', block: 'start' });\n"
                + "    }\n"
                + "  });\n"
                + "});\n";
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String number(Integer value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
