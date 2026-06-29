package com.kuocai.cdn.api.wechat.scancode.api;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.kuocai.cdn.api.wechat.scancode.service.WeChatCodeService;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.vo.WeChatCodeConfigVo;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutTextMessage;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * @author xiaobo
 * @date 2023/4/10
 */
@Controller
@Slf4j
public class WechatScanCodeToLoginApi extends BaseController {

    @Resource
    private WxMpService wxService;

    @Resource
    private WeChatCodeService weChatCodeService;

    /**
     * 微信配置token
     */
    @Value("${wx.token}")
    private String wxToken;

    /**
     * 二维码过期时间
     */
    @Value("${wx.code.expiretime}")
    private Integer codeExpireTime;

    /**
     * redis存取的oppenid的过期时间
     */
    @Value("${wx.redis.openid.expiretime}")
    private Integer openIdRedisExpireTime;

    /**
     * 获取微信扫码二维码
     */
    @PostMapping("/getWechatQrCode")
    @ResponseBody
    public RespResult getWechatQrCode() throws BusinessException {
        if (!wechatLoginEnabled()) {
            return RespResult.fail("微信登录未启用，请先在后台配置快捷登录");
        }
        refreshWxServiceConfig();
        // 设置场景id，登录的时候需要根据这个获取openid
        String sceneStr = IdUtil.getSnowflake().nextIdStr();
        String codeImg = null;
        try {
            // 获取临时二维码
            WxMpQrCodeTicket wxMpQrCodeTicket = wxService.getQrcodeService().qrCodeCreateTmpTicket(sceneStr, codeExpireTime);
            codeImg = wxService.getQrcodeService().qrCodePictureUrl(wxMpQrCodeTicket.getTicket());
        } catch (WxErrorException e) {
            throw new BusinessException("获取微信二维码失败，{}", e.getMessage()).setCause(e).log();
        }
        return RespResult.success(codeImg, sceneStr);
    }

    /**
     * 微信扫码登录
     */
    @PostMapping("wechatOpenIdLogin")
    @ResponseBody
    public RespResult wechatOpenIdLogin(String sceneStr, HttpServletRequest request) throws BusinessException {
        Boolean exists = JedisUtil.exists(sceneStr);
        if (exists) {
            RespResult result = weChatCodeService.wechatLogin(JedisUtil.getStr(sceneStr), request);
            if (result.isSuccess() && result.getData() instanceof String && !"fail".equals(result.getData())) {
                addAuthCookie((String) result.getData(), false, request);
                result.setData("success");
            }
            return result;
        } else {
            return RespResult.fail(sceneStr);
        }
    }

    /**
     * 微信绑定
     */
    @PostMapping("wechatBinding")
    @ResponseBody
    public RespResult wechatBinding(String sceneStr) {
        Boolean exists = JedisUtil.exists(sceneStr);
        return exists ? weChatCodeService.wechatBinding(JedisUtil.getStr(sceneStr), loginUser) : RespResult.fail(sceneStr);
    }

    /**
     * 微信解绑
     */
    @PostMapping("wechatUnBinding")
    @ResponseBody
    public RespResult wechatUnBinding() {
        return weChatCodeService.wechatUnBinding(loginUser);
    }


    /**
     * 微信公众号事件推送
     */
    @RequestMapping("/sign")
    @ResponseBody
    public String wechatSign(HttpServletResponse response, HttpServletRequest request) throws Exception {
        String method = request.getMethod();
        // 微信加密签名
        String signature = request.getParameter("signature");
        // 随机字符串
        String echostr = request.getParameter("echostr");
        // 时间戳
        String timestamp = request.getParameter("timestamp");
        // 随机数
        String nonce = request.getParameter("nonce");
        //签名验证是get请求
        if ("GET".equals(method)) {
            String[] str = {wechatToken(), timestamp, nonce};
            // 字典排序
            Arrays.sort(str);
            String bigStr = str[0] + str[1] + str[2];
            // SHA1加密
            String digest = KuocaiBaseUtil.sha1(bigStr);
            // 确认请求来至微信
            if (digest.equals(signature)) {
                response.getWriter().print(echostr);
            }
            return "";
        } else {
            //将xml文件转成易处理的map(下方贴出)
            WxMpXmlMessage wxMpXmlMessage = WxMpXmlMessage.fromXml(request.getInputStream());
            log.info("================================================================");
            log.info(wxMpXmlMessage.toString());
            log.info("================================================================");
            //OpenId
            String fromUserName = wxMpXmlMessage.getFromUser();
            //消息类型，event
            String msgType = wxMpXmlMessage.getMsgType();
            //事件类型
            String event = wxMpXmlMessage.getEvent();
            String ticket = wxMpXmlMessage.getTicket();
            String sceneStr = "";
            WxMpXmlOutTextMessage texts = WxMpXmlOutTextMessage
                    .TEXT()
                    .toUser(wxMpXmlMessage.getFromUser())
                    .fromUser(wxMpXmlMessage.getToUser())
                    .content("括彩云智能CDN登录成功！")
                    .build();
            String result = texts.toXml();
            log.info("微信返回验签:msgType:{},event:{},fromUserName:{},ticket:{}", msgType, event, fromUserName, ticket);
            if (WxConsts.XmlMsgType.EVENT.equals(msgType)) {
                if (event.equals(WxConsts.EventType.SUBSCRIBE)) {
                    if (ticket != null) {
                        sceneStr = wxMpXmlMessage.getEventKey().replace("qrscene_", "");
                    }
                }
                //注：事件类型为SCAN即已关注
                else if (event.equals(WxConsts.EventType.SCAN)) {
                    if (ticket != null) {
                        sceneStr = wxMpXmlMessage.getEventKey();
                    }
                }
                // 如果sceneStr不为空代表用户已经扫描并且关注了公众号
                if (CharSequenceUtil.isNotEmpty(sceneStr)) {
                    // 将微信公众号用户ID缓存到redis中，标记用户已经扫码完成，执行登录逻辑。
                    JedisUtil.setStr(sceneStr, fromUserName, openIdRedisExpireTime);
                }
            }
            log.info("返回结果成功：{}", result);
            return result;
        }
    }

    private boolean wechatLoginEnabled() {
        WeChatCodeConfigVo config = SystemConfig.weChatCodeConfig;
        return Assert.notEmpty(config)
                && Integer.valueOf(1).equals(config.getWechatStatus())
                && Assert.notEmpty(config.getAppId())
                && Assert.notEmpty(config.getAppSecret());
    }

    private void refreshWxServiceConfig() {
        WeChatCodeConfigVo config = SystemConfig.weChatCodeConfig;
        WxMpDefaultConfigImpl wxConfig = new WxMpDefaultConfigImpl();
        wxConfig.setAppId(config.getAppId());
        wxConfig.setSecret(config.getAppSecret());
        wxService.setWxMpConfigStorage(wxConfig);
    }

    private String wechatToken() {
        WeChatCodeConfigVo config = SystemConfig.weChatCodeConfig;
        if (Assert.notEmpty(config) && Assert.notEmpty(config.getToken())) {
            return config.getToken();
        }
        return wxToken;
    }
}
