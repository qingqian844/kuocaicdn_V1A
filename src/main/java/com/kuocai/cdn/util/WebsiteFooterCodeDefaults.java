package com.kuocai.cdn.util;

import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import com.kuocai.cdn.vo.WebsiteFooterCodeConfigVo;

/**
 * Editable default footer code shown in the admin editor.
 */
public class WebsiteFooterCodeDefaults {

    private WebsiteFooterCodeDefaults() {
    }

    public static WebsiteFooterCodeConfigVo build(WebsiteBaseConfigVo config) {
        WebsiteFooterCodeConfigVo vo = new WebsiteFooterCodeConfigVo();
        vo.setEnabled(false);
        vo.setHtmlCode(defaultHtml(config));
        vo.setCssCode(defaultCss());
        vo.setJsCode(defaultJs());
        return vo;
    }

    private static String defaultHtml(WebsiteBaseConfigVo config) {
        String logo = text(config == null ? null : config.getWebsiteLogoImg(), "/front/assets/svg/logos/logo-white.svg");
        String icp = text(config == null ? null : config.getIcpNumber(), "请填写备案号");
        String wechatQrCode = text(config == null ? null : config.getWechatQrCodeImg(), "/common/images/59872277_1707142593.jpeg");
        String qqGroupQrCode = text(config == null ? null : config.getQqGroupQrCodeImg(), "/common/images/4583752_1709117981.jpeg");

        return ""
                + "<footer id=\"footer\" class=\"custom-site-footer\">\n"
                + "  <div class=\"container pb-1 pb-lg-5\">\n"
                + "    <div class=\"row content-space-t-2\">\n"
                + "      <div class=\"col-lg-3 mb-7 mb-lg-0\">\n"
                + "        <div class=\"mb-5\"><a class=\"navbar-brand\" href=\"/\" aria-label=\"Logo\"><img class=\"navbar-brand-logo\" src=\"" + escape(logo) + "\" alt=\"Logo\"></a></div>\n"
                + "        <ul class=\"list-unstyled list-py-1\">\n"
                + "          <li><a class=\"link-sm link-light\" href=\"#\"><i class=\"bi-geo-alt-fill me-1\"></i> 黑龙江省大庆市高新区新科南路3号人才大厦</a></li>\n"
                + "          <li><a class=\"link-sm link-light\" href=\"tel:0459-6662886\"><i class=\"bi-telephone-inbound-fill me-1\"></i> 0459-6662886</a></li>\n"
                + "        </ul>\n"
                + "      </div>\n"
                + "      <div class=\"col-sm mb-7 mb-sm-0\">\n"
                + "        <h5 class=\"text-white mb-3\">快速入口</h5>\n"
                + "        <ul class=\"list-unstyled list-py-1 mb-0\">\n"
                + "          <li><a class=\"link-sm link-light\" href=\"/register\">用户注册</a></li>\n"
                + "          <li><a class=\"link-sm link-light\" href=\"/user-login\">用户登录</a></li>\n"
                + "          <li><a class=\"link-sm link-light\" href=\"/product_price\">Metered pricing</a></li>\n"
                + "          <li><a class=\"link-sm link-light\" href=\"/dashboard\">控制台 <i class=\"bi-box-arrow-up-right small ms-1\"></i></a></li>\n"
                + "        </ul>\n"
                + "      </div>\n"
                + "      <div class=\"col-sm mb-7 mb-sm-0\">\n"
                + "        <h5 class=\"text-white mb-3\">服务与支持</h5>\n"
                + "        <ul class=\"list-unstyled list-py-1 mb-0\">\n"
                + "          <li><a class=\"link-sm link-light\" href=\"/order-create\">工单系统</a></li>\n"
                + "          <li><a class=\"link-sm link-light\" href=\"/operation-logs\">操作日志</a></li>\n"
                + "          <li><a class=\"link-sm link-light\" href=\"/contact\">联系我们</a></li>\n"
                + "        </ul>\n"
                + "      </div>\n"
                + "      <div class=\"col-sm\">\n"
                + "        <div class=\"row\">\n"
                + "          <div class=\"col-6 text-center\"><div style=\"max-width: 180px;\"><img src=\"" + escape(wechatQrCode) + "\" alt=\"公众号二维码\" class=\"img-fluid\" loading=\"lazy\"></div><h6 class=\"text-white mt-3\">公众号</h6></div>\n"
                + "          <div class=\"col-6 text-center\"><div style=\"max-width: 180px;\"><img src=\"" + escape(qqGroupQrCode) + "\" alt=\"QQ群二维码\" class=\"img-fluid\" loading=\"lazy\"></div><h6 class=\"text-white mt-3\">QQ 群</h6></div>\n"
                + "        </div>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "    <div class=\"border-top border-white-10 my-7\"></div>\n"
                + "    <div class=\"row\"><div class=\"col\"><p class=\"text-white mb-0\"><span>友情链接：</span> <a href=\"https://www.aiboce.com/\" title=\"AI拨测\" target=\"_blank\">AI拨测</a> &emsp; <a href=\"https://tance.cc/\" title=\"探测网\" target=\"_blank\">探测网</a> &emsp; <a href=\"https://6ke.li/whois/\" title=\"WHOIS查询\" target=\"_blank\">WHOIS查询</a></p></div></div>\n"
                + "    <div class=\"border-top border-white-10 my-7\"></div>\n"
                + "    <div class=\"w-md-85 text-lg-center mx-lg-auto\">\n"
                + "      <p class=\"text-white-50 small\">Copyright © 2025. 版权所有 黑龙江括彩科技有限公司<br><br>《ICP备案号》 <a href=\"https://beian.miit.gov.cn/\">" + escape(icp) + "</a> 《增值电信业务经营许可证》 <a href=\"https://dxzhgl.miit.gov.cn/dxxzsp/xkz/xkzgl/resource/qiyesearch.jsp?num=%E4%B8%8A%E6%B5%B7%E6%8B%AC%E5%BD%A9%E7%A7%91%E6%8A%80%E6%9C%89%E9%99%90%E5%85%AC%E5%8F%B8&type=xuke\">B1-20220812/沪B2-20220232</a></p>\n"
                + "      <p class=\"text-white-50 small\">24 小时违法和不良信息举报热线：0459-6662886，举报邮箱：baixiong@88.com</p>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "</footer>\n";
    }

    private static String defaultCss() {
        return ""
                + ".custom-site-footer { background: #132144; }\n"
                + ".custom-site-footer .navbar-brand-logo { max-width: 150px; max-height: 60px; }\n"
                + ".custom-site-footer a { color: rgba(255,255,255,.72); }\n"
                + ".custom-site-footer a:hover { color: #fff; }\n";
    }

    private static String defaultJs() {
        return "";
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
