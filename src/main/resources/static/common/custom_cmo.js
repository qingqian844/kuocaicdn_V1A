/** 更新用户信息*/
async function updateUserInfo() {
    let userName = $('#userName').val();
    let realName = $('#realName').val();
    let myWebsite = $('#myWebsite').val();
    let userPhone = $('#phoneLabel').val();
    let userEmail = $('#emailLabel').val();
    let id = $('#id').val();
    if (!emailReg(userEmail)) {
        layerWarn("请输入正确的邮箱地址");
        return;
    }
    if (!phoneReg(userPhone)) {
        layerWarn("请输入正确的手机号码");
        return;
    }
    if (!userName || !myWebsite) {
        layerWarn("基本信息不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysUser/updateBaseInfo", {
            id: id,
            userName: userName,
            realName: realName,
            myWebsite: myWebsite,
            phone: userPhone,
            userEmail: userEmail
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 更新用户密码*/
async function updatePassword() {
    let id = $('#id').val();
    let oldPass = $('#currentPasswordLabel').val();
    let password1 = $('#newPassword').val();
    let password2 = $('#confirmNewPasswordLabel').val();
    if (!oldPass || !password1 || !password2) {
        layerWarn("请完整填写信息");
        return;
    }
    if (!userPasswordReg(password1)) {
        layerWarn("密码格式不正确");
        return;
    }

    if (password1 !== password2) {
        layerWarn("两次密码不一致");
        return;
    }
    if (oldPass === password1) {
        layerWarn("密码不能和原来一致");
        return;
    }

    let data = await sendRequest(
        "POST",
        "SysUser/updatePwd", {
            id: id,
            oldPwd: oldPass,
            userPwd: password1,
        });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            if (data['data'] === 1) {
                window.location.href = 'kuocaiadmin'
            } else if (data['data'] === 2) {
                window.location.href = 'user-login'
            } else {
                window.location.href = '/'
            }
        }, 1000);
    }
}

/** 人工充值或扣款*/
async function manualRecharge() {
    let rechargeAmount = $('#rechargeAmount').val();
    let orderType = $('#orderType').val();
    let userId = $('#newSelect').find("option:selected").val();
    let userName = $('#newSelect').find("option:selected").text();
    // let amount = $('#newSelect').find("option:selected").attr("accountBalance");
    let amount = $('#nowBalance').val();
    if (!numberReg(rechargeAmount)) {
        layerWarn("请输入正确的金额");
        return;
    }
    if (rechargeAmount > 1000000) {
        layerWarn("单次最多一次性操作100万！");
        return;
    }
    console.log(rechargeAmount)
    console.log(amount)
    if (("admin_balance_deduction" == orderType) && (parseFloat(rechargeAmount) > parseFloat(amount))) {
        layerWarn("扣除金额不能大于当前余额")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysUserAccount/manualOrReduceAccount", {
            rechargeAmount: rechargeAmount,
            userId: userId,
            payType: orderType,
            userName: userName
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新网站基本设置*/
async function saveWebsiteBaseConfig() {
    let formdata = new FormData();
    appendWebsiteImageFormData(formdata, "websiteIconImgUrl", "websiteIconImg", "#websiteIconImg", "#updateWebsiteIconFile");
    if (!appendWebsiteLogoFormData(formdata)) {
        return;
    }
    appendWebsiteImageFormData(formdata, "wechatQrCodeImgUrl", "wechatQrCodeImg", "#wechatQrCodeImg", "#updateWechatQrCodeFile");
    appendWebsiteImageFormData(formdata, "qqGroupQrCodeImgUrl", "qqGroupQrCodeImg", "#qqGroupQrCodeImg", "#updateQqGroupQrCodeFile");
    formdata.append("websiteName", $('#websiteName').val())
    formdata.append("websiteAnnouncement", $('#websiteAnnouncement').val())
    formdata.append("defaultFlowPrice", $('#defaultFlowPrice').val())
    formdata.append("maxDomainCount", $('#maxDomainCount').val())
    formdata.append("inviteRewardGb", 0)
    formdata.append("invitedRewardGb", 0)
    formdata.append("monthGiftGb", 0)
    formdata.append("maxDomainCountProxy", 0)
    formdata.append("icpNumber", $('#icpNumber').val())
    formdata.append("edgeoneDomainQuotaEnabled", false)
    formdata.append("edgeoneFreeDomainQuota", 0)
    formdata.append("edgeoneDomainQuotaPrice", 0)
    formdata.append("edgeoneDomainQuotaValidDays", 0)
    formdata.append("defaultUserRoute", $('#defaultUserRoute').val() || '')
    formdata.append("overseasEnabledRoutes", $('.overseasEnabledRoute:checked').map(function () { return this.value }).get().join(','))
    formdata.append("globalEnabledRoutes", $('.globalEnabledRoute:checked').map(function () { return this.value }).get().join(','))
    formdata.append("httpsRequestFeeEnabled", $('#httpsRequestFeeEnabled').is(':checked'))
    formdata.append("httpsRequestFeeRoutes", $('.httpsRequestFeeRoute:checked').map(function () { return this.value }).get().join(','))
    formdata.append("httpsRequestFeeUnitCount", $('#httpsRequestFeeUnitCount').val() || 10000)
    formdata.append("httpsRequestFeeUnitPrice", $('#httpsRequestFeeUnitPrice').val() || 0)
    let expireTime = $('#expireTime').val();
    if (!integerReg(expireTime)) {
        layerWarn("请输入正整数")
        return;
    }
    formdata.append("expireTime", expireTime)
    let data = await sendFileUploadRequest("SysConfig/saveWebsiteBaseConfig", formdata);
    autoLayer(data);
    setTimeout(reload, 1000);
}

function appendWebsiteLogoFormData(formdata) {
    const fileInput = document.getElementById("updateWebsiteLogoFile");
    const file = fileInput && fileInput.files ? fileInput.files[0] : null;
    if (file) {
        formdata.append("websiteLogoImgUrl", "false");
        formdata.append("websiteLogoImg", file);
        return true;
    }

    const logoUrl = ($('#websiteLogoUrl').val() || '').trim();
    if (logoUrl && !isValidWebsiteLogoUrl(logoUrl)) {
        layerWarn("Logo 链接必须使用 http、https 或站内绝对路径");
        return false;
    }
    formdata.append("websiteLogoImgUrl", logoUrl || "false");
    return true;
}

function isValidWebsiteLogoUrl(value) {
    if (!value || /[\s<>"']/.test(value)) {
        return false;
    }
    if (value.startsWith('/') && !value.startsWith('//')) {
        return !value.includes('..') && !value.includes('\\');
    }
    try {
        const parsed = new URL(value);
        return (parsed.protocol === 'http:' || parsed.protocol === 'https:') && !!parsed.hostname;
    } catch (e) {
        return false;
    }
}

function handleWebsiteLogoFileChange() {
    const input = document.getElementById('updateWebsiteLogoFile');
    if (input && input.files && input.files.length > 0) {
        $('#websiteLogoUrl').val('');
    }
}

function handleWebsiteLogoUrlInput() {
    const logoUrl = ($('#websiteLogoUrl').val() || '').trim();
    const fileInput = document.getElementById('updateWebsiteLogoFile');
    if (logoUrl && fileInput) {
        fileInput.value = '';
    }
}

function previewWebsiteLogoUrl() {
    const logoUrl = ($('#websiteLogoUrl').val() || '').trim();
    if (!isValidWebsiteLogoUrl(logoUrl)) {
        layerWarn("请输入正确的 Logo 图片链接");
        return;
    }
    const preview = new Image();
    preview.onload = function () {
        $('#websiteLogoImg').attr('src', logoUrl);
        const fileInput = document.getElementById('updateWebsiteLogoFile');
        if (fileInput) {
            fileInput.value = '';
        }
    };
    preview.onerror = function () {
        layerWarn("Logo 图片链接无法加载");
    };
    preview.src = logoUrl;
}

function clearWebsiteLogo() {
    $('#websiteLogoUrl').val('');
    $('#websiteLogoImg').attr('src', '/front/assets/svg/logos/logo-white.svg');
    const fileInput = document.getElementById('updateWebsiteLogoFile');
    if (fileInput) {
        fileInput.value = '';
    }
}

function appendWebsiteImageFormData(formdata, urlField, fileField, imageSelector, fileSelector) {
    let imageSrc = $(imageSelector).attr("src") || "";
    let fileInput = $(fileSelector).get(0);
    let file = fileInput && fileInput.files ? fileInput.files[0] : null;
    if (file) {
        formdata.append(urlField, "false");
        formdata.append(fileField, file);
        return;
    }
    if (imageSrc && !imageSrc.startsWith("data:")) {
        formdata.append(urlField, imageSrc);
    } else {
        formdata.append(urlField, "false");
    }
}

/** 恢复默认首页前端代码 */
function loadDefaultWebsiteHomeCode() {
    $('#homeHtmlCode').val($('#defaultHomeHtmlCode').val());
    $('#homeCssCode').val($('#defaultHomeCssCode').val());
    $('#homeJsCode').val($('#defaultHomeJsCode').val());
    layerSuccess("已恢复默认首页代码，请保存后生效");
}

/** 保存或更新首页前端代码 */
async function saveWebsiteHomeCodeConfig() {
    let enabled = $('#homeCodeEnabled').prop("checked");
    let htmlCode = $('#homeHtmlCode').val();
    let cssCode = $('#homeCssCode').val();
    let jsCode = $('#homeJsCode').val();
    if (enabled && !htmlCode) {
        layerWarn("启用自定义首页时，HTML 内容不能为空");
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveWebsiteHomeCodeConfig", {
            enabled: enabled,
            htmlCode: htmlCode,
            cssCode: cssCode,
            jsCode: jsCode
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 恢复默认底部前端代码 */
function loadDefaultWebsiteFooterCode() {
    $('#footerHtmlCode').val($('#defaultFooterHtmlCode').val());
    $('#footerCssCode').val($('#defaultFooterCssCode').val());
    $('#footerJsCode').val($('#defaultFooterJsCode').val());
    layerSuccess("已恢复默认底部代码，请保存后生效");
}

/** 保存或更新底部前端代码 */
async function saveWebsiteFooterCodeConfig() {
    let enabled = $('#footerCodeEnabled').prop("checked");
    let htmlCode = $('#footerHtmlCode').val();
    let cssCode = $('#footerCssCode').val();
    let jsCode = $('#footerJsCode').val();
    if (enabled && !htmlCode) {
        layerWarn("启用自定义底部时，HTML 内容不能为空");
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveWebsiteFooterCodeConfig", {
            enabled: enabled,
            htmlCode: htmlCode,
            cssCode: cssCode,
            jsCode: jsCode
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}


/** 保存或更新网站权限配置*/
async function saveWebsitePermissionConfig() {
    // 这里有坑，只能使用prop("checked")，直接使用checked会返回undefined
    let forceRealAuthentication = $('#forceRealAuthentication').prop("checked");
    let forceBindingTel = $('#forceBindingTel').prop("checked");
    let closeRegister = $('#closeRegister').prop("checked");
    let data = await sendRequest(
        "POST",
        "SysConfig/saveWebsitePermissionConfig", {
            forceRealAuthentication: forceRealAuthentication,
            forceBindingTel: forceBindingTel,
            closeRegister: closeRegister
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新SEO优化配置*/
async function saveWebsiteSeoConfig() {
    let homeTitle = $('#homeTitle').val();
    let searchKeyword = $('#searchKeyword').val();
    let websiteInfo = $('#websiteInfo').val();
    let data = await sendRequest(
        "POST",
        "SysConfig/saveWebsiteSeoConfig", {
            homeTitle: homeTitle,
            searchKeyword: searchKeyword,
            websiteInfo: websiteInfo
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新联系方式配置*/
async function saveWebsiteContactConfig() {
    let websiteTel = $('#websiteTel').val();
    let websiteEmail = $('#websiteEmail').val();
    let websiteQq = $('#websiteQq').val();
    let company = $('#company').val();
    if (!websiteTel || !websiteEmail || !websiteQq || !company) {
        layerWarn("所有选项都不能为空");
        return;
    }
    if (!phoneReg(websiteTel)) {
        layerWarn("请输入正确的电话号");
        return;
    }
    if (!emailReg(websiteEmail)) {
        layerWarn("请输入正确的邮箱");
        return;
    }
    if (!qqReg(websiteQq)) {
        layerWarn("请输入正确的QQ号");
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveWebsiteContactConfig", {
            websiteTel: websiteTel,
            websiteEmail: websiteEmail,
            websiteQq: websiteQq,
            company: company
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新同意协议配置*/
async function saveWebsiteAgreementConfig() {
    let content = document.querySelector('#user-protocol').children[0].innerHTML;
    let data = await sendRequest(
        "POST",
        "SysConfig/saveWebsiteAgreementConfig", {
            agreementInfo: content
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新站点访问根目录配置*/
async function saveWebsiteAccessRootConfig() {
    let serverRoot = $('#serverRoot').val();
    let clientRoot = $('#clientRoot').val();
    let clientUrl = $('#clientUrl').val();
    if (!serverRoot || !clientRoot || !clientUrl) {
        layerWarn("所有选项都不能为空");
        return;
    }
    if (!urlReg(clientUrl)) {
        layerWarn("请输入正确的客户端地址");
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveWebsiteAccessRootConfig", {
            serverRoot: serverRoot,
            clientRoot: clientRoot,
            clientUrl: clientUrl
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}


/** 保存或更新腾讯人脸配置*/
async function saveTencentConfig() {
    let secretKey = $('#secretKey').val();
    let secretId = $('#secretId').val();
    let certificationFee = $('#certificationFee').val();
    let ruleId = $('#ruleId').val();
    if (!secretKey || !secretId || !ruleId || !certificationFee) {
        layerWarn("所有选项都不能为空")
        return;
    }
    if (!numberReg(certificationFee)) {
        layerWarn("请输入正确的金额");
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveTencentFaceConfig", {
            secretKey: secretKey,
            secretId: secretId,
            certificationFee: certificationFee,
            ruleId: ruleId
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新支付宝实名认证*/
async function saveAlipayAuthenticationConfig() {
    let publicKeyAlipay = $('#publicKeyAlipay').val();
    let alipayCertificationUrl = $('#alipayCertificationUrl').val();
    let privateKeyAlipay = $('#privateKeyAlipay').val();
    let gatewayUrlAlipay = $('#gatewayUrlAlipay').val();
    let signTypeAlipay = $('#signTypeAlipay').val();
    let charsetAlipay = $('#charsetAlipay').val();
    let appIdAlipay = $('#appIdAlipay').val();
    let formatAlipay = $('#formatAlipay').val();
    if (!publicKeyAlipay || !alipayCertificationUrl || !privateKeyAlipay || !gatewayUrlAlipay || !signTypeAlipay || !appIdAlipay || !charsetAlipay || !formatAlipay) {
        layerWarn("所有选项都不能为空")
        return;
    }
    if (!urlReg(gatewayUrlAlipay)) {
        layerWarn("请输入正确的网关地址")
        return;
    }
    if (!urlReg(alipayCertificationUrl)) {
        layerWarn("请输入正确的认证地址")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveAliPayAuthenticationConfig", {
            appIdAlipay: appIdAlipay,
            privateKeyAlipay: privateKeyAlipay,
            publicKeyAlipay: publicKeyAlipay,
            gatewayUrlAlipay: gatewayUrlAlipay,
            charsetAlipay: charsetAlipay,
            signTypeAlipay: signTypeAlipay,
            alipayCertificationUrl: alipayCertificationUrl,
            formatAlipay: formatAlipay
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新邮箱配置*/
async function saveEmailServiceConfig() {
    let smtpServer = $('#smtpServer').val();
    let senderMailbox = $('#senderMailbox').val();
    let senderTitle = $('#senderTitle').val();
    let authorizationPassword = $('#authorizationPassword').val();
    let serverPort = $('#serverPort').val();
    if (!smtpServer || !senderMailbox || !authorizationPassword || !serverPort) {
        layerWarn("所有选项都不能为空")
        return;
    }
    if (!emailReg(senderMailbox)) {
        layerWarn("请输入正确的邮箱地址");
        return;
    }
    if (!senderTitle) {
        layerWarn("请输入发件人标题");
        return;
    }
    if (!portReg(serverPort)) {
        layerWarn("请输入正确的端口");
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveEmailConfig", {
            smtpServer: smtpServer,
            senderMailbox: senderMailbox,
            senderTitle: senderTitle,
            authorizationPassword: authorizationPassword,
            serverPort: serverPort
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 邮箱模版配置*/
async function saveEmailTemplateConfig() {
    let notifyTemplateContent = $('#notifyTemplateContent').val();
    let forgetPasswordTemplateContent = $('#forgetPasswordTemplateContent').val();
    let packetExpirationTemplateContent = $('#packetExpirationTemplateContent').val();
    let packetWillExpirationTemplateContent = $('#packetWillExpirationTemplateContent').val();
    let packetGiveOutTemplateContent = $('#packetGiveOutTemplateContent').val();
    let packetWillGiveOutTemplateContent = $('#packetWillGiveOutTemplateContent').val();
    let workOrderMessageTemplateContent = $('#workOrderMessageTemplateContent').val();
    // 这里有坑，只能使用prop("checked")，直接使用checked会返回undefined
    // let safeVerifyStatus = $('#safeVerifyStatus').prop("checked");
    let data = await sendRequest(
        "POST",
        "SysConfig/saveEmailTemplateConfig", {
            packetWillExpirationTemplateContent: packetWillExpirationTemplateContent,
            forgetPasswordTemplateContent: forgetPasswordTemplateContent,
            packetGiveOutTemplateContent: packetGiveOutTemplateContent,
            packetWillGiveOutTemplateContent: packetWillGiveOutTemplateContent,
            notifyTemplateContent: notifyTemplateContent,
            packetExpirationTemplateContent: packetExpirationTemplateContent,
            workOrderMessageTemplateContent: workOrderMessageTemplateContent
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新短信配置*/
async function saveSmsServiceConfig() {
    let smsSign = $('#smsSign').val();
    let secretKey = $('#secretKey').val();
    let sdkAppId = $('#sdkAppId').val();
    if (!sdkAppId || !secretKey || !smsSign) {
        layerWarn("所有选项都不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveSmsConfig", {
            smsSign: smsSign,
            secretKey: secretKey,
            sdkAppId: sdkAppId
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 短信模版配置*/
async function saveSmsTemplateConfig() {
    let notifyTemplateTitle = $('#notifyTemplateTitle').val();
    let packetExpirationTemplateTitle = $('#packetExpirationTemplateTitle').val();
    let packetWillExpirationTemplateTitle = $('#packetWillExpirationTemplateTitle').val();
    let packetGiveOutTemplateTitle = $('#packetGiveOutTemplateTitle').val();
    let packetWillGiveOutTemplateTitle = $('#packetWillGiveOutTemplateTitle').val();
    let forgetPasswordTemplateTitle = $('#forgetPasswordTemplateTitle').val();
    // 这里有坑，只能使用prop("checked")，直接使用checked会返回undefined
    // let safeVerifyStatus = $('#safeVerifyStatus').prop("checked");
    let data = await sendRequest(
        "POST",
        "SysConfig/saveSmsTemplateConfig", {
            notifyTemplateTitle: notifyTemplateTitle,
            packetExpirationTemplateTitle: packetExpirationTemplateTitle,
            packetWillExpirationTemplateTitle: packetWillExpirationTemplateTitle,
            packetGiveOutTemplateTitle: packetGiveOutTemplateTitle,
            forgetPasswordTemplateTitle: forgetPasswordTemplateTitle,
            packetWillGiveOutTemplateTitle: packetWillGiveOutTemplateTitle
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}


/** 保存或更新华为云API 配置*/
async function saveHuaWeiCloudConfig() {
    let huaWeiCloudProjectName = $('#huaWeiCloudProjectName').val();
    let huaWeiCloudAk = $('#huaWeiCloudAk').val();
    let huaWeiCloudSk = $('#huaWeiCloudSk').val();
    if (!huaWeiCloudProjectName || !huaWeiCloudAk || !huaWeiCloudSk) {
        layerWarn("所有选项都不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveHuaWeiCloudConfig", {
            huaWeiCloudProjectName: huaWeiCloudProjectName,
            huaWeiCloudAk: huaWeiCloudAk,
            huaWeiCloudSk: huaWeiCloudSk
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新火山云API 配置*/
async function saveVolcanicCloudConfig() {
    let volcanicCloudProjectName = $('#volcanicCloudProjectName').val();
    let volcanicCloudAk = $('#volcanicCloudAk').val();
    let volcanicCloudSk = $('#volcanicCloudSk').val();
    if (!volcanicCloudAk || !volcanicCloudSk) {
        layerWarn("所有选项都不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveVolcanicCloudConfig", {
            volcanicCloudProjectName: volcanicCloudProjectName,
            volcanicCloudAk: volcanicCloudAk,
            volcanicCloudSk: volcanicCloudSk
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新白山云API 配置*/
async function saveWhiteMountainCloudConfig() {
    let whiteMountainCloudProjectName = $('#whiteMountainCloudProjectName').val();
    let whiteMountainCloudBaseApi = $('#whiteMountainCloudBaseApi').val();
    let whiteMountainCloudToken = $('#whiteMountainCloudToken').val();
    if (!whiteMountainCloudProjectName || !whiteMountainCloudBaseApi || !whiteMountainCloudToken) {
        layerWarn("所有选项都不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveWhiteMountainCloudConfig", {
            whiteMountainCloudProjectName: whiteMountainCloudProjectName,
            whiteMountainCloudBaseApi: whiteMountainCloudBaseApi,
            whiteMountainCloudToken: whiteMountainCloudToken
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新腾讯云API 配置*/
async function saveTencentCloudConfig() {
    const tencentCloudProjectName = $('#tencentCloudProjectName').val();
    const tencentCloudSecretId = $('#tencentCloudSecretId').val();
    const tencentCloudSecretKey = $('#tencentCloudSecretKey').val();
    if (!tencentCloudProjectName || !tencentCloudSecretId || !tencentCloudSecretKey) {
        layerWarn("所有选项都不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveTencentCloudConfig", {
            tencentCloudProjectName: tencentCloudProjectName,
            tencentCloudSecretId: tencentCloudSecretId,
            tencentCloudSecretKey: tencentCloudSecretKey
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新腾讯云 EdgeOne API 配置*/
async function saveTencentEdgeOneConfig() {
    const projectName = $('#tencentEdgeOneProjectName').val();
    const tagValue = $('#tencentEdgeOneTagValue').val();
    const secretId = $('#tencentEdgeOneSecretId').val();
    const secretKey = $('#tencentEdgeOneSecretKey').val();
    const planId = $('#tencentEdgeOnePlanId').val();
    if (!projectName || !tagValue || !secretId || !secretKey || !planId) {
        layerWarn("所有选项都不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveTencentEdgeOneConfig", {
            projectName: projectName,
            tagValue: tagValue,
            secretId: secretId,
            secretKey: secretKey,
            planId: planId
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新 CDNetworks API 配置*/
async function saveCDNetworksConfig() {
    const cdnetworksProjectName = $('#cdnetworksProjectName').val();
    const cdnetworksAccessKey = $('#cdnetworksAccessKey').val();
    const cdnetworksSecretKey = $('#cdnetworksSecretKey').val();
    if (!cdnetworksProjectName || !cdnetworksAccessKey || !cdnetworksSecretKey) {
        layerWarn("所有选项都不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveCDNetworksConfig", {
            cdnetworksProjectName: cdnetworksProjectName,
            cdnetworksAccessKey: cdnetworksAccessKey,
            cdnetworksSecretKey: cdnetworksSecretKey
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新 Aliyun Cdn API 配置*/
async function saveAliyunCdnConfig() {
    const aliyunCdnProjectName = $('#aliyunCdnProjectName').val();
    const aliyunCdnAccessKeyId = $('#aliyunCdnAccessKeyId').val();
    const aliyunCdnAccessKeySecret = $('#aliyunCdnAccessKeySecret').val();
    if (!aliyunCdnProjectName || !aliyunCdnAccessKeyId || !aliyunCdnAccessKeySecret) {
        layerWarn("所有选项都不能为空")
        return;
    }
    const data = await sendRequest(
        "POST",
        "SysConfig/saveAliyunCdnConfig", {
            projectName: aliyunCdnProjectName,
            accessKeyId: aliyunCdnAccessKeyId,
            accessKeySecret: aliyunCdnAccessKeySecret
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新 Kingsoft CDN API 配置*/
async function saveKingsoftCdnConfig() {
    const kingsoftCdnAccessKey = $('#kingsoftCdnAccessKey').val();
    const kingsoftCdnSecretKey = $('#kingsoftCdnSecretKey').val();
    const kingsoftCdnEndpoint = $('#kingsoftCdnEndpoint').val();
    const kingsoftCdnRegion = $('#kingsoftCdnRegion').val();
    const kingsoftCdnServiceName = $('#kingsoftCdnServiceName').val();
    const kingsoftCdnProjectId = $('#kingsoftCdnProjectId').val();

    if (!kingsoftCdnAccessKey || !kingsoftCdnSecretKey || !kingsoftCdnEndpoint || !kingsoftCdnRegion || !kingsoftCdnServiceName) {
        layerWarn("所有选项都不能为空")
        return;
    }

    const data = await sendRequest(
        "POST",
        "SysConfig/saveKingsoftCdnConfig", {
            accessKey: kingsoftCdnAccessKey,
            secretKey: kingsoftCdnSecretKey,
            endpoint: kingsoftCdnEndpoint,
            region: kingsoftCdnRegion,
            serviceName: kingsoftCdnServiceName,
            projectId: kingsoftCdnProjectId
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新 网宿 Cdn API 配置*/
async function saveWangsuCdnConfig() {
    const wangsuCdnAccessKey = $('#wangsuCdnAccessKey').val();
    const wangsuCdnSecretKey = $('#wangsuCdnSecretKey').val();
    if (!wangsuCdnAccessKey || !wangsuCdnSecretKey) {
        layerWarn("所有选项都不能为空")
        return;
    }
    const data = await sendRequest(
        "POST",
        "SysConfig/saveWangsuCdnConfig", {
            accessKey: wangsuCdnAccessKey,
            secretKey: wangsuCdnSecretKey
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新 百度 Cdn API 配置*/
async function saveBaiduCdnConfig() {
    const baiduCdnAccessKeyId = $('#baiduCdnAccessKeyId').val();
    const baiduCdnSecretAccessKey = $('#baiduCdnSecretAccessKey').val();
    if (!baiduCdnAccessKeyId || !baiduCdnSecretAccessKey) {
        layerWarn("所有选项都不能为空")
        return;
    }
    const data = await sendRequest(
        "POST",
        "SysConfig/saveBaiduCdnConfig", {
            accessKeyId: baiduCdnAccessKeyId,
            secretAccessKey: baiduCdnSecretAccessKey
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/**
 * 保存融合CDN配置
 * @returns {Promise<void>}
 */
async function saveMergeCdnApiConfig() {
    const huaweiWorkHours = $('#huaweiWorkHours').val();
    const volcanicWorkHours = $('#volcanicWorkHours').val();
    if (!huaweiWorkHours || !volcanicWorkHours) {
        layerWarn("所有选项都不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveMergeCdnApiConfig", {
            huaweiWorkHours: huaweiWorkHours,
            volcanicWorkHours: volcanicWorkHours
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新微信扫码登录注册配置*/
async function saveWechatCodeConfig() {
    let wechatStatus = $('#wechatCodeStatus').prop("checked");
    let appId = $('#appId').val();
    let appSecret = $('#appSecret').val();
    let token = $('#wechatToken').val();
    let notifyUrl = $('#notifyUrl').val();
    if (wechatStatus && (!appSecret || !appId || !token)) {
        layerWarn("启用微信登录时，AppID、AppSecret、Token 都不能为空")
        return;
    }
    if (notifyUrl && !urlReg(notifyUrl)) {
        layerWarn("请输入正确的回调地址")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveWechatCodeConfig", {
            wechatStatus: wechatStatus ? 1 : 0,
            appId: appId,
            appSecret: appSecret,
            token: token,
            notifyUrl: notifyUrl
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 保存或更新API 配置*/
async function saveApiConfig() {
    let accessKeyId = $('#accessKeyId').val();
    let secretId = $('#secretId').val();
    if (!accessKeyId || !secretId) {
        layerWarn("所有选项都不能为空")
        return;
    }
    let data = await sendRequest(
        "POST",
        "SysConfig/saveApiConfig", {
            accessKeyId: accessKeyId,
            secretId: secretId
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 人工扣款/充值，下拉框选择后赋值方法*/
async function saveDnsConfig() {
    const primaryDomain = $('#dnsPrimaryDomain').val();
    const domainTtl = $('#dnsDomainTtl').val();
    const secretId = $('#dnsSecretId').val();
    const secretKey = $('#dnsSecretKey').val();
    if (!primaryDomain || !secretId || !secretKey) {
        layerWarn("CNAME主域名、SecretId、SecretKey 都不能为空");
        return;
    }
    const data = await sendRequest(
        "POST",
        "SysConfig/saveDnsConfig", {
            primaryDomain: primaryDomain,
            domainTtl: domainTtl || '600',
            selectDns: 'tencent',
            secretId: secretId,
            secretKey: secretKey
        });
    autoLayer(data);
    setTimeout(reload, 1000);
}

function setBalance() {
    let amount = $('#newSelect').find("option:selected").attr("accountBalance");
    $('#nowBalance').val(amount)
}

/** 获取微信二维码*/
async function getWechatQrCode(isLogin) {
    let data = await sendRequest(
        "POST",
        "getWechatQrCode", {});
    if (data['code'] === 'SUCCESS') {
        $('#qrcodes').attr('src', data['message'])
        setTimeout($('#testButton').click(), 1000);
        isLogin ? wechatLogin(data['data']) : wechatBinding(data['data']);
    } else {
        layerFail(data['message'])
        setTimeout(reload, 1000);
    }
}

/** 微信扫码登录*/
async function wechatLogin(sceneStr) {
    let timer = null;
    let data = await sendRequest(
        "POST",
        "wechatOpenIdLogin", {
            sceneStr: sceneStr
        }, 'application/x-www-form-urlencoded; charset=UTF-8', loading = false);
    if (data['code'] !== "SUCCESS") {
        timer = setTimeout(() => {
            wechatLogin(sceneStr);
        }, 2000)
    } else {
        console.log(data['data'])
        clearTimeout(timer);
        if (data['data'] !== 'fail') {
            layerSuccess('登录成功，正在跳转...');
            setTimeout(function() {
                window.location.href = 'dashboard'
            }, 1000)
        } else {
            layerWarn(data['message']);
            setTimeout(function() {
                window.location.href = 'register'
            }, 1000)
        }
    }
}

/** 微信扫码绑定*/
async function wechatBinding(sceneStr) {
    let timer = null;
    let data = await sendRequest(
        "POST",
        "wechatBinding", {
            sceneStr: sceneStr
        }, 'application/x-www-form-urlencoded; charset=UTF-8', loading = false);
    if (data['code'] !== "SUCCESS") {
        timer = setTimeout(() => {
            wechatBinding(sceneStr);
        }, 2000)
    } else {
        clearTimeout(timer);
        if (data['data'] === 'success') {
            layerSuccess(data['message']);
        } else {
            layerWarn(data['message']);
        }
        setTimeout(reload, 1000);

    }
}

/** 微信解除绑定*/
async function wechatUnBinding() {
    let timer = null;
    let data = await sendRequest(
        "POST",
        "wechatUnBinding", {});
    autoLayer(data);
    setTimeout(reload, 2000);
}

/** 站内消息发送*/
async function saveMessage() {
    let receiveUserId = $('#userId').find("option:selected").val();
    let message = $('#messageDetail').val();
    if (!receiveUserId || !message) {
        layerWarn("接收人和消息不能为null")
        return;
    }
    let data = await sendRequest(
        "POST",
        "Message/saveMessage", {
            receiveUserId: receiveUserId,
            message: message
        });
    autoLayer(data);
    setTimeout(reload, 2000);
}

async function oneButtonPay() {
    let data = await sendRequest("POST", "TransactionOrder/useBalanceOneButtonPay", {});
    autoLayer(data);
}

/** 获取汇总信息*/
async function getCollectOrder(isAdmin) {
    let userId = null;
    if (isAdmin) {
        userId = $('#userId').find("option:selected").val();
    }
    console.log(userId)
    let data = await sendRequest(
        "POST",
        "TransactionOrder/getCollectOrder", {
            userId: userId
        });
    let collectData = data['data'];
    console.log(collectData)
    let nowAmount = 0;
    let yesterdayAmount = 0;
    let sevenAmount = 0;
    let thirtyAmount = 0;
    let allAmount = 0;
    if (collectData) {
        if (collectData['nowAmount']) {
            nowAmount = collectData['nowAmount'];
        }
        if (collectData['yesterdayAmount']) {
            yesterdayAmount = collectData['yesterdayAmount'];
        }
        if (collectData['sevenAmount']) {
            sevenAmount = collectData['sevenAmount'];
        }
        if (collectData['thirtyAmount']) {
            thirtyAmount = collectData['thirtyAmount'];
        }
        if (collectData['allAmount']) {
            allAmount = collectData['allAmount'];
        }
    }
    $('#nowAmount').val(nowAmount);
    $('#yesterdayAmount').val(yesterdayAmount);
    $('#sevenAmount').val(sevenAmount);
    $('#thirtyAmount').val(thirtyAmount);
    $('#allAmount').val(allAmount);

}


/** 关闭微信扫码登录页面*/
function closeWechatQrcode() {
    setTimeout(reload, 1000);
}

/**
 * 确认解除绑定操作
 * @param callback
 */
function confirmUnbind(callback) {
    let confirm = `
        <div id="confirmModal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title" id="exampleModalCenterTitle"><i class="bi bi-exclamation-circle-fill text-danger"></i> 微信解除绑定操作</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body">
                <p>解除绑定后不可扫码登录，确定解绑吗？</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-white" data-bs-dismiss="modal">取消</button>
                <button type="button" class="btn btn-primary" onclick="${callback}">确认</button>
              </div>
            </div>
          </div>
        </div>
    `
    $('body').append($(confirm));
    turnModal('confirmModal', 'on')
}

/**
 * 确认发布公告操作
 * @param callback
 */
function confirmPublish(callback) {
    let confirm = `
        <div id="confirmModal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title" id="exampleModalCenterTitle"><i class="bi bi-exclamation-circle-fill text-danger"></i> 系统公告操作</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body">
                <p>发布此条公告将立马生效，之前发布的变为历史，确定发布吗？</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-white" data-bs-dismiss="modal">取消</button>
                <button type="button" class="btn btn-primary" onclick="${callback}">确认</button>
              </div>
            </div>
          </div>
        </div>
    `
    $('body').append($(confirm));
    turnModal('confirmModal', 'on')
}

/**
 * 确认撤回操作
 * @param callback
 */
function confirmToWithdraw(callback) {
    let confirm = `
        <div id="confirmModal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title" id="exampleModalCenterTitle"><i class="bi bi-exclamation-circle-fill text-danger"></i> 撤回操作</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body">
                <p>确定撤回赠送给用户的流量吗？</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-white" data-bs-dismiss="modal">取消</button>
                <button type="button" class="btn btn-primary" onclick="${callback}">确认</button>
              </div>
            </div>
          </div>
        </div>
    `
    $('body').append($(confirm));
    turnModal('confirmModal', 'on')
}


/**
 * 确认退款操作
 * @param callback
 */
function confirmToUserRefund(callback) {
    let confirm = `
        <div id="confirmModal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title" id="exampleModalCenterTitle"><i class="bi bi-exclamation-circle-fill text-danger"></i> 退款操作</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body">
                <p>确定退款此流量包吗？</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-white" data-bs-dismiss="modal">取消</button>
                <button type="button" class="btn btn-primary" onclick="${callback}">确认</button>
              </div>
            </div>
          </div>
        </div>
    `
    $('body').append($(confirm));
    turnModal('confirmModal', 'on')
}
