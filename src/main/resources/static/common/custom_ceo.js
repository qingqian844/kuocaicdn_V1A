/**
 * 获取密码
 */
async function getPassword(o) {
    const data = await sendRequest("POST", "login/getPassword", o)
    autoLayer(data)
}

async function sendPasswordResetCode(o) {
    return await sendRequest("POST", "login/sendPasswordResetCode", o)
}

async function resetPassword(o) {
    return await sendRequest("POST", "login/resetPassword", o)
}

async function faceVerify() {
    const data = await sendRequest("POST", "/FaceCertifyVerify/query", {})
    if (data['code'] === 'SUCCESS') {
        setTimeout(reload, 1e3)
    }
    autoLayer(data)
}

async function getAuthCode() {
    const realNameLabel = $("#realNameLabel").val()
    const idCardNumLabel = $("#idCardNumLabel").val()
    const phoneLabel = $("#phoneLabel").val()
    if (!realNameLabel) {
        layerWarn("姓名不可为空")
        return;
    }
    if (!idCardNumLabel) {
        layerWarn("证件号不可为空")
        return;
    }
    if (!phoneLabel) {
        layerWarn("手机号不可为空")
        return;
    }
    const data = await sendRequest("POST", "/FaceCertifyVerify/verify", {
        'realName': realNameLabel,
        'idCardNum': idCardNumLabel,
        'phone': phoneLabel
    });
    $("#qrCode").html("");
    if (data['code'] === 'SUCCESS') {
        const url = window.location.origin + data['data'];
        $('#qrCode').qrcode({
            render: "canvas",
            text: url,
            correctLevel: QRErrorCorrectLevel.M,
            width: "150", // 二维码的宽度
            height: "150", // 二维码的高度
            background: "#ffffff", // 二维码的后景色
            foreground: "#000000" // 二维码的前景色
        })
        $('#aliCode').show();
        $('#codeBtn .btn').removeClass('btn-primary').addClass('btn-secondary').text('重新获取');
        $('#enterpriseAuthDiv').hide();
        $('#faceVerifyDiv').show();
    }
    autoLayer(data);
}

/**
 * 用户登录
 */
async function loginUser(callback) {
    $("#loginBtn").attr({ "disabled": "disabled" })
    let userAccount = $('#userAccount').val();
    let userPwd = $('#userPassword').val();
    let remember = $('#remember').is(':checked');
    if (!userAccount) {
        layerWarn("账户不能为空");
        $('#loginBtn').removeAttr("disabled");
        return;
    }
    if (userAccount.search("@") !== -1) {
        if (!emailReg(userAccount)) {
            layerWarn("请输入正确的邮箱地址");
            $('#loginBtn').removeAttr("disabled");
            return;
        }
    } else {
        if (!phoneReg(userAccount)) {
            layerWarn("请输入正确的手机号");
            $('#loginBtn').removeAttr("disabled");
            return;
        }
    }
    if (!userPasswordReg(userPwd)) {
        layerWarn("密码格式错误！8-16字符，支持字母、数字以及特殊符号");
        $('#loginBtn').removeAttr("disabled");
        return;
    }

    let data = await sendRequest("POST", "login/loginUser", {
        userAccount: userAccount,
        userPwd: userPwd,
        remember: remember,
    });
    if (data['code'] !== 'SUCCESS') {
        console.log(data);
        autoLayer(data)
        $('#loginBtn').removeAttr("disabled");
    } else {
        layerSuccess('登录成功，正在跳转...');
        setTimeout(function() {
            safeRedirect(callback, 'dashboard')
        }, 1000);
    }
}

async function uploadAgentLogo() {
    let file = $('#agentLogo').get(0).files[0];
    let formData = new FormData();
    formData.append('logo', file);
    formData.append('id', $('#configId').val());
    let data = await sendFileUploadRequest("RemovedFeature/disabled", formData, false)
    console.log(data);
    if (data['code'] === 'SUCCESS') {
        $('#agentLogo').val('');
        $('#agentLogoImg').attr('src', data['data']);
        $('#agentLogoImg').show();
    }
}

async function uploadAgentLogoDashboard() {
    let file = $('#agentLogoDashboard').get(0).files[0];
    let formData = new FormData();
    formData.append('logo', file);
    formData.append('id', $('#configId').val());
    let data = await sendFileUploadRequest("RemovedFeature/disabled", formData, false)
    console.log(data);
    if (data['code'] === 'SUCCESS') {
        $('#agentLogoDashboard').val('');
        $('#agentLogoDashboardImg').attr('src', data['data']);
        $('#agentLogoDashboardImg').show();
    }
}

async function uploadAgentIcon() {
    let file = $('#agentIcon').get(0).files[0];
    let formData = new FormData();
    formData.append('icon', file);
    formData.append('id', $('#configId').val());
    let data = await sendFileUploadRequest("RemovedFeature/disabled", formData, false)
    console.log(data);
    if (data['code'] === 'SUCCESS') {
        $('#agentIcon').val('');
        $('#agentIconImg').attr('src', data['data']);
        $('#agentIconImg').show();
    }
}

async function saveAgentConfig() {
    let agentLogoImg = $('#agentLogoImg').attr('src');
    let agentLogoDashboardImg = $('#agentLogoDashboardImg').attr('src');
    let configId = $('#configId').val();
    let domain = $('#domain').val();
    let email = $('#email').val();
    let phone = $('#phone').val();
    let company = $('#company').val();
    let address = $('#address').val();
    let icp = $('#icp').val();
    let licence = $('#licence').val();
    let licenceUrl = $('#licenceUrl').val();
    let wechatServiceUrl = $('#wechatServiceUrl').val();
    let websiteName = $('#websiteName').val();
    let websiteKeyword = $('#websiteKeyword').val();
    let websiteDescription = $('#websiteDescription').val();
    let title = $('#title').val();
    let about = $('#about').val();

    // 邮件服务
    let smtpServer = $('#smtpServer').val();
    let senderMailbox = $('#senderMailbox').val();
    let senderTitle = $('#senderTitle').val();
    let authorizationPassword = $('#authorizationPassword').val();
    let serverPort = $('#serverPort').val();
    const emailConfig = {
            smtpServer: smtpServer,
            senderMailbox: senderMailbox,
            senderTitle: senderTitle,
            authorizationPassword: authorizationPassword,
            serverPort: serverPort
        }
        // 邮件模板
    let notifyTemplateContent = $('#notifyTemplateContent').val();
    let forgetPasswordTemplateContent = $('#forgetPasswordTemplateContent').val();
    let packetExpirationTemplateContent = $('#packetExpirationTemplateContent').val();
    let packetWillExpirationTemplateContent = $('#packetWillExpirationTemplateContent').val();
    let packetGiveOutTemplateContent = $('#packetGiveOutTemplateContent').val();
    let packetWillGiveOutTemplateContent = $('#packetWillGiveOutTemplateContent').val();
    const emailTemplateConfig = {
            packetWillExpirationTemplateContent: packetWillExpirationTemplateContent,
            forgetPasswordTemplateContent: forgetPasswordTemplateContent,
            packetGiveOutTemplateContent: packetGiveOutTemplateContent,
            packetWillGiveOutTemplateContent: packetWillGiveOutTemplateContent,
            notifyTemplateContent: notifyTemplateContent,
            packetExpirationTemplateContent: packetExpirationTemplateContent,
        }
        // 短信服务
    let smsSign = $('#smsSign').val();
    let secretKey = $('#secretKey').val();
    let sdkAppId = $('#sdkAppId').val();
    const smsConfig = {
            smsSign: smsSign,
            secretKey: secretKey,
            sdkAppId: sdkAppId
        }
        // 短信模板
    let notifyTemplateTitle = $('#notifyTemplateTitle').val();
    let packetExpirationTemplateTitle = $('#packetExpirationTemplateTitle').val();
    let packetWillExpirationTemplateTitle = $('#packetWillExpirationTemplateTitle').val();
    let packetGiveOutTemplateTitle = $('#packetGiveOutTemplateTitle').val();
    let packetWillGiveOutTemplateTitle = $('#packetWillGiveOutTemplateTitle').val();
    let forgetPasswordTemplateTitle = $('#forgetPasswordTemplateTitle').val();
    const smsTemplateConfig = {
        notifyTemplateTitle: notifyTemplateTitle,
        packetExpirationTemplateTitle: packetExpirationTemplateTitle,
        packetWillExpirationTemplateTitle: packetWillExpirationTemplateTitle,
        packetGiveOutTemplateTitle: packetGiveOutTemplateTitle,
        forgetPasswordTemplateTitle: forgetPasswordTemplateTitle,
        packetWillGiveOutTemplateTitle: packetWillGiveOutTemplateTitle
    }

    let params = {
        id: configId,
        agentLogoImg: agentLogoImg,
        email: email,
        phone: phone,
        company: company,
        address: address,
        icp: icp,
        licence: licence,
        licenceUrl: licenceUrl,
        wechatServiceUrl: wechatServiceUrl,
        websiteName: websiteName,
        websiteKeyword: websiteKeyword,
        websiteDescription: websiteDescription,
        title: title,
        about: about,
        domain: domain,
        emailConfig: JSON.stringify(emailConfig),
        emailTemplateConfig: JSON.stringify(emailTemplateConfig),
        smsConfig: JSON.stringify(smsConfig),
        smsTemplateConfig: JSON.stringify(smsTemplateConfig),
    }
    let data = await sendRequest("POST", "RemovedFeature/disabled", params);
    autoLayer(data);
    setTimeout(reload, 1000);
}

/**
 * 管理员登录
 */
async function loginAdmin() {
    $("#loginBtn").attr({ "disabled": "disabled" })
    let userAccount = $('#userAccount').val();
    let userPwd = $('#userPassword').val();
    let remember = $('#remember').is(':checked');
    if (!userAccount) {
        layerWarn("账户不能为空");
        $('#loginBtn').removeAttr("disabled");
        return;
    }
    if (userAccount.search("@") !== -1) {
        if (!emailReg(userAccount)) {
            layerWarn("请输入正确的邮箱地址");
            $('#loginBtn').removeAttr("disabled");
            return;
        }
    } else {
        if (!phoneReg(userAccount)) {
            layerWarn("请输入正确的手机号");
            $('#loginBtn').removeAttr("disabled");
            return;
        }
    }
    if (!userPasswordReg(userPwd)) {
        layerWarn("密码格式错误！8-16字符，支持字母、数字以及特殊符号");
        $('#loginBtn').removeAttr("disabled");
        return;
    }

    let data = await sendRequest("POST", "login/loginAdmin", {
        userAccount: userAccount,
        userPwd: userPwd,
        remember: remember,
    });
    if (data['code'] !== 'SUCCESS') {
        autoLayer(data)
        $('#loginBtn').removeAttr("disabled");
    } else {
        layerSuccess('登录成功，正在跳转...');
        setTimeout(function() {
            window.location.href = 'dashboard'
        }, 1000);
    }
}

/**
 * 退出登录
 */
async function logout() {
    let data = await sendRequest("POST", "logout", {});
    autoLayer(data)
    console.log(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            window.location.href = '.'
        }, 1000);
    }
}

/**
 * 发送邮箱验证码
 */
async function sendEmailCode(v) {
    let userEmail = $('#userEmail').val();
    if (!emailReg(userEmail)) {
        layerWarn("请输入正确的邮箱地址");
        return;
    }
    let data = await sendRequest("POST", "login/sendEmailCode", Object.assign({
        userEmail: userEmail
    }, v));
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        time('#sendEmailCodeBtn', 60);
        turnModal('emailModal', 'on');
    }
}

/**
 * 绑定邮箱
 */
async function bindingEmail() {
    let c1 = $('#emailSrCodeInput1').val();
    let c2 = $('#emailSrCodeInput2').val();
    let c3 = $('#emailSrCodeInput3').val();
    let c4 = $('#emailSrCodeInput4').val();
    if (!c1 || !c2 || !c3 || !c4) {
        return;
    }
    let code = c1 + c2 + c3 + c4

    let userEmail = $('#userEmail').val();
    if (!emailReg(userEmail)) {
        layerWarn("请输入正确的邮箱地址");
        return;
    }
    let data = await sendRequest("POST", "SysUser/updateEmail", {
        email: userEmail,
        code: code
    });
    autoLayer(data);
    setTimeout(reload, 1000);
}

/**
 * 验证域名解析
 */
const DOMAIN_CREATE_PENDING_KEY = 'kuocaiPendingDomainCreate';
let domainCreateInProgress = false;

function savePendingDomainCreate(payload) {
    try {
        localStorage.setItem(DOMAIN_CREATE_PENDING_KEY, JSON.stringify({
            domainName: payload.domainName,
            startedAt: Date.now()
        }));
    } catch (e) {
    }
}

function clearPendingDomainCreate() {
    try {
        localStorage.removeItem(DOMAIN_CREATE_PENDING_KEY);
    } catch (e) {
    }
}

function showDomainCreateProgress(message) {
    domainCreateInProgress = true;
    let overlay = document.getElementById('domainCreateProgressOverlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'domainCreateProgressOverlay';
        overlay.style.cssText = 'position:fixed;inset:0;z-index:20050;background:rgba(20,28,45,.58);display:flex;align-items:center;justify-content:center;padding:20px;';
        overlay.innerHTML = '<div style="width:min(440px,92vw);background:#fff;border-radius:14px;padding:30px 26px;text-align:center;box-shadow:0 18px 55px rgba(0,0,0,.22)">' +
            '<div class="spinner-border text-primary mb-3" role="status" aria-hidden="true"></div>' +
            '<h4 class="mb-2">正在创建加速域名</h4>' +
            '<p id="domainCreateProgressMessage" class="text-muted mb-2"></p>' +
            '<p class="small text-warning mb-0">请勿刷新、关闭页面或重复点击，系统会自动保存创建进度。</p>' +
            '</div>';
        document.body.appendChild(overlay);
    }
    const messageElement = document.getElementById('domainCreateProgressMessage');
    if (messageElement) {
        messageElement.textContent = message || '正在向云厂商提交配置，通常需要几十秒，请耐心等待。';
    }
    $('#createDomainBtn, #dnsVerifyDomainBtn, #fileVerifyDomainBtn').each(function() {
        if (this.dataset.domainCreateOriginalDisabled === undefined) {
            this.dataset.domainCreateOriginalDisabled = this.disabled ? '1' : '0';
        }
        this.disabled = true;
    });
}

function hideDomainCreateProgress() {
    domainCreateInProgress = false;
    const overlay = document.getElementById('domainCreateProgressOverlay');
    if (overlay) {
        overlay.remove();
    }
    $('#createDomainBtn, #dnsVerifyDomainBtn, #fileVerifyDomainBtn').each(function() {
        this.disabled = this.dataset.domainCreateOriginalDisabled === '1';
        delete this.dataset.domainCreateOriginalDisabled;
    });
}

function queryDomainCreateStatus(domainName) {
    return new Promise(function(resolve) {
        $.ajax({
            type: 'GET',
            url: 'CdnDomain/createStatus',
            data: { domainName: domainName },
            dataType: 'json',
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            success: function(data) { resolve(data); },
            error: function() { resolve(null); }
        });
    });
}

function waitDomainCreateInterval(milliseconds) {
    return new Promise(function(resolve) { setTimeout(resolve, milliseconds); });
}

async function waitForDomainCreateRecord(domainName, attempts = 15) {
    for (let i = 0; i < attempts; i++) {
        const status = await queryDomainCreateStatus(domainName);
        if (status && status.code === 'SUCCESS' && status.data) {
            return status.data;
        }
        await waitDomainCreateInterval(2000);
    }
    return null;
}

async function recoverInterruptedDomainCreate(domainName) {
    showDomainCreateProgress('页面连接已中断，正在查询服务器保存的创建进度…');
    const domain = await waitForDomainCreateRecord(domainName);
    if (domain) {
        clearPendingDomainCreate();
        domainCreateInProgress = false;
        layerSuccess('创建记录已恢复，正在进入域名列表…');
        setTimeout(function() { window.location.href = 'domain-list'; }, 500);
        return true;
    }
    clearPendingDomainCreate();
    hideDomainCreateProgress();
    layerWarn('暂未找到创建记录，请确认信息后重新点击创建');
    return false;
}

window.addEventListener('beforeunload', function(event) {
    if (!domainCreateInProgress) {
        return;
    }
    event.preventDefault();
    event.returnValue = '';
});

async function verifyDomainRecord(domainName, verifyType = 'dns', serviceArea, afterSuccess) {
    if (domainCreateInProgress) {
        return;
    }
    showDomainCreateProgress('正在检查 TXT 或文件验证记录，请稍候…');
    let data;
    try {
        data = await sendRequest("POST", "CdnDomain/verifyDomainRecord", {
            area: serviceArea,
            domainName: domainName,
            verifyType: verifyType
        }, 'application/x-www-form-urlencoded; charset=UTF-8', false)
    } catch (e) {
        hideDomainCreateProgress();
        return;
    }
    if (data['code'] === 'SUCCESS') {
        autoLayer(data);
        if (typeof afterSuccess === 'function') {
            hideDomainCreateProgress();
            await afterSuccess();
        } else {
            hideDomainCreateProgress();
            $('#verifyDomainModal').modal('hide');
            setTimeout(function() {
                window.location.href = 'domain-create'
            }, 1e3);
        }
        return;
    }
    hideDomainCreateProgress();
    autoLayer(data);
}

function showDomainVerifyModal(domainName, serviceArea, info, pendingCreatePayload) {
    info = info || {}
    const verifyDomainName = (info && (info['domainName'] || info['rootDomain'])) || domainName
    const modalInstance = new bootstrap.Modal('#verifyDomainModal', {
        backdrop: 'static'
    })
    const verifyDomainModal = $('#verifyDomainModal')
    verifyDomainModal.find('.txt-verify-domain').text(verifyDomainName)
    verifyDomainModal.find('.txt-sub-domain').text(info['subDomain'] || '')
    verifyDomainModal.find('.txt-record-type').text(info['recordType'] || '')
    verifyDomainModal.find('.txt-record').text(info['record'] || '')
    const verifyDomains = info['fileVerifyDomains'] || []
    verifyDomainModal.find('.txt-verify-domains').text(verifyDomains.join(' 或 '))
    var verifyLink = document.createElement('a')
    verifyLink.href = info['fileVerifyUrl'] || '#'
    verifyLink.textContent = info['fileVerifyUrl'] || ''
    verifyLink.target = '_blank'
    verifyDomainModal.find('.txt-verify-url').html(verifyLink)
    var blob = new Blob([info['content'] || ''], { type: 'text/plain;charset=utf-8' }),
        link = document.createElement('a')
    var fileUrl = URL.createObjectURL(blob)
    link.href = fileUrl
    link.download = info['fileVerifyName'] || 'domain-verify.txt'
    link.textContent = info['fileVerifyName'] || 'domain-verify.txt'
    verifyDomainModal.find('.txt-verify-file').html(link)
    verifyDomainModal.find('#dnsVerifyDomainBtn').off('click').on('click', function() {
        if (pendingCreatePayload) {
            verifyDomainRecord(verifyDomainName, 'dns', serviceArea, async function() {
                await retryCreateDomainAfterVerify(pendingCreatePayload)
            })
        } else {
            verifyDomainRecord(verifyDomainName, 'dns', serviceArea)
        }
    })
    verifyDomainModal.find('#fileVerifyDomainBtn').off('click').on('click', function() {
        if (pendingCreatePayload) {
            verifyDomainRecord(verifyDomainName, 'file', serviceArea, async function() {
                await retryCreateDomainAfterVerify(pendingCreatePayload)
            })
        } else {
            verifyDomainRecord(verifyDomainName, 'file', serviceArea)
        }
    })
    verifyDomainModal.one('hidden.bs.modal', function() {
        verifyDomainModal.find('#dnsVerifyDomainBtn').off('click')
        verifyDomainModal.find('#fileVerifyDomainBtn').off('click')
        URL.revokeObjectURL(fileUrl)
    })
    modalInstance.show()
}

/**
 * 域名解析校验
 */
async function createVerifyRecord(domainName, serviceArea, pendingCreatePayload) {
    const data = await sendRequest("POST", "CdnDomain/createVerifyRecord", {
        area: serviceArea,
        domainName: domainName
    })
    if (data['code'] === 'SUCCESS') {
        showDomainVerifyModal((data['data'] && data['data']['domainName']) || domainName, serviceArea, data['data'], pendingCreatePayload)
    } else {
        autoLayer(data);
    }
}

/**
 * 创建加速域名
 */
function shouldShowDomainVerifyModal(response) {
    if (!response) {
        return false;
    }
    if (response.data && (response.data.needVerify === true || response.data.needVerify === 'true')) {
        return true;
    }
    const message = String(response.message || '');
    const lower = message.toLowerCase();
    if (message.indexOf('无权限') !== -1
        || lower.indexOf('unauthorized') !== -1
        || lower.indexOf('not authorized') !== -1
        || lower.indexOf('createaccelerationdomain') !== -1) {
        return false;
    }
    return lower.indexOf('verify') !== -1
        || lower.indexOf('owner') !== -1
        || message.indexOf('验证') !== -1
        || message.indexOf('归属') !== -1;
}

async function retryCreateDomainAfterVerify(payload) {
    return submitDomainCreate(payload, true);
}

async function submitDomainCreate(payload, verified = false) {
    if (domainCreateInProgress) {
        return;
    }
    savePendingDomainCreate(payload);
    showDomainCreateProgress(verified
        ? '验证已通过，正在向云厂商创建域名并保存到本站，请耐心等待…'
        : '正在向云厂商创建域名并保存到本站，通常需要几十秒，请耐心等待…');
    let data;
    try {
        data = await sendRequest("POST", "CdnDomain/create", payload,
            'application/x-www-form-urlencoded; charset=UTF-8', false);
    } catch (e) {
        await recoverInterruptedDomainCreate(payload.domainName);
        return;
    }
    if (data['code'] === 'SUCCESS') {
        clearPendingDomainCreate();
        domainCreateInProgress = false;
        layerSuccess('创建成功，正在跳转...');
        setTimeout(function() {
            window.location.href = 'domain-list'
        }, 1000);
        return;
    }
    clearPendingDomainCreate();
    hideDomainCreateProgress();
    if (shouldShowEdgeOneQuotaModal(data)) {
        $('#verifyDomainModal').modal('hide');
        showEdgeOneQuotaModal(data);
        return;
    }
    autoLayer(data);
    if (data['message'] === '当前页面已失效，请刷新页面后再试') {
        setTimeout(function() { window.location.href = 'domain-create'; }, 1000);
        return;
    }
    if (shouldShowDomainVerifyModal(data)) {
        if (data.data && data.data.verifyInfo) {
            const verifyDomainName = data.data.domainName || data.data.verifyDomainName || data.data.verifyInfo.domainName || payload.domainName;
            showDomainVerifyModal(verifyDomainName, payload.serviceArea, data.data.verifyInfo, payload);
        } else {
            await createVerifyRecord((data.data && data.data.domainName) || payload.domainName, payload.serviceArea, payload);
        }
    }
}

async function createDomain() {
    let domainName = $('#domainName').val();
    let verifyCode = $('#verifyCode').text();
    let businessType = $('input[name="businessType"]:radio:checked').val();
    let serviceArea = $('input[name="serviceArea"]:radio:checked').val();
    console.log(serviceArea);
    let originType = $('input[name="originType"]:radio:checked').val();
    let originAddr = $('#originAddr').val();
    let originProtocol = $('input[name="createOriginProtocol"]:radio:checked').val();
    let httpPort = $('#originHttpPort').val();
    let httpsPort = $('#originHttpsPort').val();
    let originWeight = $('#originWeight').val();
    let originHost = $('#originHost').val();
    if (!domainName || !domainReg(domainName) || hasScheme(domainName)) {
        layerWarn("请正确填写加速域名");
        errorShake('domainName')
        return;
    }
    if (!originAddr) {
        layerWarn("源站信息不能为空");
        errorShake('originAddr')
        return;
    }
    if (!validPort(httpPort)) {
        layerWarn("HTTP端口必须在1-65535范围内");
        errorShake('originHttpPort');
        return;
    }
    if (!validPort(httpsPort)) {
        layerWarn("HTTPS端口必须在1-65535范围内");
        errorShake('originHttpsPort');
        return;
    }
    if (originWeight && (!numberReg(originWeight) || Number(originWeight) < 1 || Number(originWeight) > 100)) {
        layerWarn("权重必须在1-100范围内");
        errorShake('originWeight');
        return;
    }
    const payload = {
        domainName: domainName.toLocaleLowerCase(),
        businessType: businessType,
        serviceArea: serviceArea,
        originType: originType,
        originAddr: originAddr,
        originProtocol: originProtocol,
        httpPort: httpPort,
        httpsPort: httpsPort,
        originHost: originHost,
        originWeight: originWeight,
        verifyCode: verifyCode,
    };
    await submitDomainCreate(payload);
}

$(async function() {
    if (!document.getElementById('domainName')) {
        return;
    }
    let pending = null;
    try {
        pending = JSON.parse(localStorage.getItem(DOMAIN_CREATE_PENDING_KEY) || 'null');
    } catch (e) {
        clearPendingDomainCreate();
    }
    if (!pending || !pending.domainName) {
        return;
    }
    if (!pending.startedAt || Date.now() - pending.startedAt > 20 * 60 * 1000) {
        clearPendingDomainCreate();
        return;
    }
    await recoverInterruptedDomainCreate(pending.domainName);
});

function validPort(port) {
    if (!numberReg(port)) {
        return false;
    }
    let value = Number(port);
    return value >= 1 && value <= 65535;
}

/**
 * 启用加速域名
 * @param id
 */
function shouldShowEdgeOneQuotaModal(data) {
    if (!data) {
        return false;
    }
    if (data.data && data.data.action === 'EDGEONE_QUOTA_REQUIRED') {
        return true;
    }
    return data.message && data.message.indexOf('EdgeOne根域名额度不足') >= 0;
}

async function showEdgeOneQuotaModal(data) {
    let summary = data && data.data && data.data.summary ? data.data.summary : {};
    let price = summary.unitPrice || 30;
    let validDays = summary.quotaValidDays || 30;
    let payOptions = await loadEdgeOneQuotaPayOptions();
    let buttons = [
        '<button type="button" class="btn btn-primary" onclick="buyEdgeOneQuotaByBalance(this)">余额支付</button>'
    ];
    if (payOptions.alipayEnabled) {
        buttons.push('<button type="button" class="btn btn-outline-primary" onclick="buyEdgeOneQuotaByQr(\'alipay\', this)">支付宝扫码支付</button>');
    }
    if (payOptions.wechatEnabled) {
        buttons.push('<button type="button" class="btn btn-outline-success" onclick="buyEdgeOneQuotaByQr(\'wechat\', this)">微信扫码支付</button>');
    }
    layer.open({
        type: 1,
        title: '购买EdgeOne根域名额度',
        area: ['480px', 'auto'],
        content: `
            <div class="p-4">
                <p class="mb-2 fw-semibold">当前EdgeOne根域名额度不足，请先购买1个额度后再添加。</p>
                <p class="text-muted mb-3">价格：${price} 元，有效期：${validDays} 天。</p>
                <div id="edgeOneQuotaQrCode" class="text-center mb-3"></div>
                <div class="d-grid gap-2">
${buttons.join('')}
                </div>
            </div>`
    });
}

async function loadEdgeOneQuotaPayOptions() {
    try {
        let data = await sendRequest("POST", "TransactionOrder/queryEdgeOneDomainQuotaPayOptions", {}, 'application/x-www-form-urlencoded; charset=UTF-8', false);
        if (data && data['code'] === 'SUCCESS' && data['data']) {
            return data['data'];
        }
    } catch (e) {
    }
    return {alipayEnabled: false, wechatEnabled: false};
}
function setEdgeOneQuotaPayLoading(button, loading) {
    if (!button) {
        return;
    }
    if (loading) {
        button.dataset.originalText = button.innerHTML;
        button.disabled = true;
        button.innerHTML = 'Processing...';
        return;
    }
    button.disabled = false;
    if (button.dataset.originalText) {
        button.innerHTML = button.dataset.originalText;
    }
}

async function buyEdgeOneQuotaByBalance(button) {
    setEdgeOneQuotaPayLoading(button, true);
    let data = await sendRequest("POST", "TransactionOrder/createEdgeOneDomainQuotaBalanceOrder", {});
    setEdgeOneQuotaPayLoading(button, false);
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(reload, 1000);
    }
}

async function buyEdgeOneQuotaByQr(type, button) {
    let qrContainer = $("#edgeOneQuotaQrCode");
    qrContainer.html('<div class="text-muted small py-2">正在生成支付二维码...</div>');
    setEdgeOneQuotaPayLoading(button, true);
    let data;
    try {
        let url = type === 'wechat' ? "wechat/edgeOneDomainQuotaPrepay" : "alipay/edgeOneDomainQuotaPrepay";
        data = await sendRequest("POST", url, {}, 'application/x-www-form-urlencoded; charset=UTF-8', false);
    } catch (e) {
        setEdgeOneQuotaPayLoading(button, false);
        qrContainer.html('<div class="text-danger small py-2">支付下单请求失败，请刷新页面后重试。</div>');
        return;
    }
    setEdgeOneQuotaPayLoading(button, false);
    if (data['code'] !== 'SUCCESS') {
        autoLayer(data);
        qrContainer.html('<div class="text-danger small py-2">' + (data['message'] || '支付下单失败') + '</div>');
        return;
    }
    let qrText = data['message'] || (data['data'] && (data['data']['qrCode'] || data['data']['codeUrl']));
    if (!qrText) {
        layerFail("支付二维码内容为空，请检查支付配置");
        qrContainer.html('<div class="text-danger small py-2">支付二维码内容为空，请检查支付配置。</div>');
        return;
    }
    let orderId = data['data'] && data['data']['id'] ? data['data']['id'].toString() : "";
    showEdgeOneQuotaQrModal(type, qrText, orderId);
    if (!orderId) {
        layerWarn("支付订单创建成功，但未返回订单ID，请刷新后查看额度");
        return;
    }
    trackEdgeOneQuotaOrder(orderId);
}

function showEdgeOneQuotaQrModal(type, qrText, orderId) {
    let payName = type === 'wechat' ? '微信' : '支付宝';
    window.edgeOneQuotaTrackingClosed = false;
    layer.open({
        type: 1,
        title: '请使用' + payName + '扫码支付',
        area: ['360px', 'auto'],
        shadeClose: false,
        content: `
            <div class="p-4 text-center">
                <div id="edgeOneQuotaPayQrCanvas" class="d-inline-block p-2 bg-white border rounded"></div>
                <div id="edgeOneQuotaPayFallback" class="mt-3 d-none"></div>
                <p class="text-muted small mt-3 mb-0">支付完成后系统会自动刷新额度。</p>
            </div>`,
        success: function () {
            renderEdgeOneQuotaQr(qrText);
        },
        end: function () {
            window.edgeOneQuotaTrackingClosed = true;
        }
    });
}

function renderEdgeOneQuotaQr(qrText) {
    let canvasBox = $("#edgeOneQuotaPayQrCanvas");
    let fallbackBox = $("#edgeOneQuotaPayFallback");
    canvasBox.html("");
    fallbackBox.addClass("d-none").html("");
    try {
        if (!$.fn.qrcode) {
            throw new Error("qrcode plugin missing");
        }
        canvasBox.qrcode({
            render: "canvas",
            text: qrText,
            width: 180,
            height: 180,
            background: "#ffffff",
            foreground: "#000000"
        });
    } catch (e) {
        canvasBox.html('<div class="text-danger small p-3">二维码生成失败</div>');
        fallbackBox.removeClass("d-none")
            .append($('<textarea class="form-control mb-2" rows="3" readonly></textarea>').val(qrText));
        if (/^https?:\/\//i.test(qrText)) {
            fallbackBox.append($('<a class="btn btn-sm btn-outline-primary" target="_blank" rel="noopener">打开支付链接</a>').attr('href', qrText));
        }
    }
}

async function trackEdgeOneQuotaOrder(orderId) {
    if (window.edgeOneQuotaTrackingClosed) {
        return;
    }
    let data = await sendRequest("POST", "alipay/queryTransactions", {
        orderId: orderId
    }, 'application/x-www-form-urlencoded; charset=UTF-8', loading = false);
    if (data['code'] !== 'SUCCESS') {
        setTimeout(() => {
            trackEdgeOneQuotaOrder(orderId);
        }, 2000);
        return;
    }
    layerSuccess("购买成功，额度已到账");
    setTimeout(reload, 1000);
}

function showDomainSettingLoading(message) {
    const text = message || '正在进入配置页，请稍候';
    $('#domainSettingLoadingOverlay').remove();
    if (!$('#domainSettingLoadingStyle').length) {
        $('head').append(`
            <style id="domainSettingLoadingStyle">
                #domainSettingLoadingOverlay {
                    position: fixed;
                    inset: 0;
                    z-index: 2147483000;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: rgba(15, 23, 42, .28);
                    backdrop-filter: blur(2px);
                }
                #domainSettingLoadingOverlay .domain-setting-loading-card {
                    min-width: 260px;
                    max-width: min(360px, calc(100vw - 40px));
                    padding: 28px 30px;
                    border-radius: 10px;
                    background: #fff;
                    box-shadow: 0 18px 48px rgba(15, 23, 42, .2);
                    text-align: center;
                    color: #1f2937;
                }
                #domainSettingLoadingOverlay .domain-setting-loading-spinner {
                    width: 42px;
                    height: 42px;
                    margin: 0 auto 16px;
                    border: 4px solid #e5e7eb;
                    border-top-color: #2563eb;
                    border-radius: 50%;
                    animation: domainSettingLoadingSpin .8s linear infinite;
                }
                #domainSettingLoadingOverlay .domain-setting-loading-title {
                    font-size: 16px;
                    font-weight: 600;
                    line-height: 1.4;
                }
                #domainSettingLoadingOverlay .domain-setting-loading-desc {
                    margin-top: 8px;
                    color: #6b7280;
                    font-size: 13px;
                    line-height: 1.5;
                }
                @keyframes domainSettingLoadingSpin {
                    to { transform: rotate(360deg); }
                }
            </style>
        `);
    }
    $('body').append(`
        <div id="domainSettingLoadingOverlay">
            <div class="domain-setting-loading-card">
                <div class="domain-setting-loading-spinner"></div>
                <div class="domain-setting-loading-title">${text}</div>
                <div class="domain-setting-loading-desc">正在读取上游配置，页面会自动打开</div>
            </div>
        </div>
    `);
}

function updateDomainSettingLoading(message) {
    const $title = $('#domainSettingLoadingOverlay .domain-setting-loading-title');
    if ($title.length) {
        $title.text(message || '正在进入配置页，请稍候');
    } else {
        showDomainSettingLoading(message);
    }
}

function hideDomainSettingLoading() {
    $('#domainSettingLoadingOverlay').remove();
}

async function openDomainSettingWhenReady(id, button) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    const $button = button ? $(button) : null;
    const oldHtml = $button && $button.length ? $button.html() : '';
    let navigating = false;
    if ($button && $button.length) {
        $button.prop('disabled', true).html('<i class="bi-hourglass-split"></i> 打开中');
    }
    showDomainSettingLoading('正在检查配置状态');
    try {
        const data = await sendRequest("GET", "CdnDomain/configReady", {
            id: id
        }, 'application/x-www-form-urlencoded; charset=UTF-8', false);
        if (data['code'] === 'SUCCESS') {
            navigating = true;
            updateDomainSettingLoading('配置已就绪，正在打开页面');
            window.location.href = `domain-setting-basic?id=${id}`;
            return;
        }
        hideDomainSettingLoading();
        layerWarn(data['message'] || "上游配置还在同步中，请稍后再进入配置");
    } catch (e) {
        hideDomainSettingLoading();
        layerWarn("上游配置还在同步中，请稍后再进入配置");
    } finally {
        if (!navigating && $button && $button.length) {
            $button.prop('disabled', false).html(oldHtml);
        }
    }
}

async function retrySelfHostedDomain(id, button) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    const $button = button ? $(button) : null;
    const oldHtml = $button && $button.length ? $button.html() : '';
    if ($button && $button.length) {
        $button.prop('disabled', true).html('<i class="bi-arrow-repeat"></i> 重试中');
    }
    try {
        const data = await sendRequest("POST", "CdnDomain/retrySelfHostedConfig", {id: id});
        autoLayer(data);
        if (data['code'] === 'SUCCESS') {
            setTimeout(function() {
                reload();
            }, 800);
        }
    } finally {
        if ($button && $button.length) {
            $button.prop('disabled', false).html(oldHtml);
        }
    }
}

async function enableDomain(id) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "CdnDomain/enable", {
        id: id
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 停用加速域名
 * @param id
 */
async function disableDomain(id) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "CdnDomain/disable", {
        id: id
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 删除加速域名
 * @param id
 */
async function deleteDomain(id) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "CdnDomain/delete", {
        id: id
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 删除加速域名
 * @param id
 */
async function forceDeleteDomain(id) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "CdnDomain/forceDelete", {
        id: id
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 删除用户
 * @param id
 */
async function forceDeleteUser(id) {
    if (!id) {
        layerWarn("用户ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "SysUser/forceDelete", {
        id: id
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 封禁用户
 * @param id
 */
async function bannedUser(id, reason) {
    if (!id) {
        layerWarn("用户ID不能为空");
        return;
    }
    if (!reason) {
        layerWarn("封禁原因不能为空");
        return;
    }
    let data = await sendRequest("POST", "SysUserBanned/banned", {
        userId: id,
        reason: reason
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            window.open('banned-list', '_self')
        }, 1000);
    }
}

async function unBannedUser(id) {
    if (!id) {
        layerWarn("用户ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "SysUserBanned/unBanned", {
        userId: id
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 切换主备源站
 */
async function changeMainBackSource(id) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "CdnDomainSources/change", {
        id: id
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {

    }
    setTimeout(function() {
        reload()
    }, 1000);
}

/**
 * 修改IPV6
 */
async function changeIpv6(id) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    let status = $('#ipv6').prop("checked");
    let data = await sendRequest("POST", "CdnDomain/ipv6", {
        id: id,
        status: status === true ? 1 : 0
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {

    }
    setTimeout(function() {
        reload()
    }, 1000);
}

/**
 * 保存源站信息（包含回源协议）
 */
function normalizeOriginProtocol(protocol) {
    protocol = (protocol || '').toString().trim().toLowerCase();
    return ['http', 'https', 'follow'].includes(protocol) ? protocol : '';
}

function inferOriginProtocol(originAddr) {
    originAddr = (originAddr || '').toString().trim();
    if (/(^|[;,\s])[^;,\s]+:443(?=$|[;,\s])/.test(originAddr)) {
        return 'https';
    }
    if (/(^|[;,\s])[^;,\s]+:80(?=$|[;,\s])/.test(originAddr)) {
        return 'http';
    }
    return '';
}

async function saveSources(id) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    let backFlag = $('#backFlag').val();
    let originTypeMain = $('input[name="originType"]:radio:checked').val();
    let originAddr = $('#originAddr').val();
    let hostNameMain = $('#hostName').val();

    // 获取回源协议配置
    let originProtocol = normalizeOriginProtocol($('input[name="originProtocol"]:radio:checked').val());
    if (!originProtocol) {
        originProtocol = inferOriginProtocol(originAddr) ||
            normalizeOriginProtocol($('#currentOriginProtocol').val()) ||
            'http';
    }

    // 根据协议类型获取端口值
    let httpPortMain, httpsPortMain;

    if (originProtocol === 'http') {
        // HTTP协议：从httpPortOnly获取端口，如果不存在则从httpPort获取
        httpPortMain = $('#httpPortOnly').val() || $('#httpPort').val();
        httpsPortMain = 443; // 默认HTTPS端口
    } else if (originProtocol === 'https') {
        // HTTPS协议：从httpsPortOnly获取端口，如果不存在则从httpsPort获取
        httpsPortMain = $('#httpsPortOnly').val() || $('#httpsPort').val();
        httpPortMain = 80; // 默认HTTP端口
    } else {
        // 协议跟随：从两个输入框获取
        httpPortMain = $('#httpPort').val();
        httpsPortMain = $('#httpsPort').val();
    }

    // 基础验证
    if (!originAddr) {
        layerWarn("源站信息不能为空");
        errorShake('originAddr');
        return;
    }
    if (!hostNameMain) {
        layerWarn("回源HOST不能为空");
        errorShake('hostName');
        return;
    }
    if (!originProtocol) {
        originProtocol = 'http';
    }

    // 端口验证和处理 - 根据协议类型进行不同的验证
    let finalHttpPort = parseInt(httpPortMain) || 80;
    let finalHttpsPort = parseInt(httpsPortMain) || 443;
    let domainRouteType = ($('#domainRouteType').val() || $('#cdnRouteType').val() || '').toLowerCase();
    let portEmbeddedInOrigin = ['tencent', 'aliyun'].includes(domainRouteType);

    if (!portEmbeddedInOrigin && originProtocol === 'http') {
        // HTTP协议：只需要HTTP端口
        if (!httpPortMain) {
            layerWarn("HTTP协议模式下，HTTP回源端口不能为空");
            return;
        }
        finalHttpPort = parseInt(httpPortMain);
        if (finalHttpPort < 1 || finalHttpPort > 65535) {
            layerWarn("HTTP端口必须在1-65535范围内");
            return;
        }
    } else if (!portEmbeddedInOrigin && originProtocol === 'https') {
        // HTTPS协议：只需要HTTPS端口
        if (!httpsPortMain) {
            layerWarn("HTTPS协议模式下，HTTPS回源端口不能为空");
            return;
        }
        finalHttpsPort = parseInt(httpsPortMain);
        if (finalHttpsPort < 1 || finalHttpsPort > 65535) {
            layerWarn("HTTPS端口必须在1-65535范围内");
            return;
        }
    } else if (!portEmbeddedInOrigin && originProtocol === 'follow') {
        // 协议跟随：需要两个端口
        if (!httpPortMain || !httpsPortMain) {
            layerWarn("协议跟随模式下，HTTP和HTTPS回源端口都不能为空");
            return;
        }
        finalHttpPort = parseInt(httpPortMain);
        finalHttpsPort = parseInt(httpsPortMain);

        if (finalHttpPort < 1 || finalHttpPort > 65535 || finalHttpsPort < 1 || finalHttpsPort > 65535) {
            layerWarn("端口必须在1-65535范围内");
            return;
        }

        // 金山云要求：协议跟随时HTTP和HTTPS端口不能相同
        if (finalHttpPort === finalHttpsPort) {
            layerWarn("协议跟随模式下，HTTP和HTTPS端口不能相同");
            return;
        }
    }

    // 保存当前端口值到localStorage，以便在切换协议时使用
    try {
        localStorage.setItem(`cdn_ports_${id}`, JSON.stringify({
            httpPort: finalHttpPort,
            httpsPort: finalHttpsPort
        }));
    } catch (e) {
        console.error("Error saving ports to localStorage:", e);
    }

    let main = {
        originType: originTypeMain,
        ipOrDomain: originAddr,
        httpPort: finalHttpPort,
        httpsPort: finalHttpsPort,
        activeStandby: 1,
        hostName: hostNameMain
            // 注意：不在main对象中放originProtocol，避免重复
    };

    let back = null;
    if (backFlag === '1') {
        let originTypeBack = $('input[name="originTypeBack"]:radio:checked').val();
        let originAddrBack = $('#originAddrBack').val();
        let httpPortBack = $('#httpPortBack').val();
        let httpsPortBack = $('#httpsPortBack').val();
        let hostNameBack = $('#hostNameBack').val();

        if (!originAddrBack) {
            layerWarn("备源站信息不能为空");
            errorShake('originAddrBack');
            return;
        }
        if (!httpPortBack || !httpsPortBack) {
            layerWarn("备源站回源端口不能为空");
            return;
        }
        if (!hostNameBack) {
            layerWarn("备源站回源HOST不能为空");
            errorShake('hostNameBack');
            return;
        }

        let backHttpPort = parseInt(httpPortBack);
        let backHttpsPort = parseInt(httpsPortBack);

        if (backHttpPort < 1 || backHttpPort > 65535 || backHttpsPort < 1 || backHttpsPort > 65535) {
            layerWarn("备源站端口必须在1-65535范围内");
            return;
        }

        back = {
            originType: originTypeBack,
            ipOrDomain: originAddrBack,
            httpPort: backHttpPort,
            httpsPort: backHttpsPort,
            activeStandby: 0,
            hostName: hostNameBack
        };

        // 金山云主备源站限制提示
        if (window.currentDomainRoute === 'kingsoft') {
            // 检查是否有不同的端口或Host配置
            if (backHttpPort !== finalHttpPort || backHttpsPort !== finalHttpsPort || hostNameBack !== hostNameMain) {
                layerWarn("提醒：金山云CDN限制，主备源站将使用统一的端口和回源Host配置（以主源站为准）", 5000);
            }
        }
    }

    let param = {
        'cdnDomainId': id,
        'main': main,
        'back': back,
        'originProtocol': originProtocol // 回源协议放在顶级参数中
    }

    console.log('Saving source config:', JSON.stringify(param, null, 2));

    let data = await sendRequest("POST", "CdnDomainSources/save", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 保存源站信息
 */
async function saveSourcesBaiShan(id) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    let backFlag = $('#backFlag').val();
    let originTypeMain = $('input[name="originType"]:radio:checked').val();
    let originProtocol = $('input[name="originProtocol"]:radio:checked').val();
    let originAddr = $('#originAddr').val();
    let port = $('#portId').val();
    // let httpPortMain = $('#httpPort').val();
    // let httpsPortMain = $('#httpsPort').val();
    // let hostNameMain = $('#hostName').val();
    // 只保留主源站信息
    if (!originAddr) {
        layerWarn("源站信息不能为空");
        errorShake('originAddr');
        return;
    }
    let main = {
        originType: originTypeMain,
        ipOrDomain: originAddr,
        // httpPort: httpPortMain,
        // httpsPort: httpsPortMain,
        activeStandby: 1,
        // originProtocol: originProtocol,
    };
    let back = null;
    if (backFlag === '1') {
        let originTypeBack = $('input[name="originTypeBack"]:radio:checked').val();
        let originAddrBack = $('#originAddrBack').val();
        // let httpPortBack = $('#httpPortBack').val();
        // let httpsPortBack = $('#httpsPortBack').val();
        // let hostNameBack = $('#hostNameBack').val();
        if (!originAddrBack) {
            layerWarn("备源站信息不能为空");
            errorShake('originAddrBack');
            return;
        }
        back = {
            originType: originTypeBack,
            ipOrDomain: originAddrBack,
            // httpPort: httpPortBack,
            // httpsPort: httpsPortBack,
            activeStandby: 0,
            // hostName: hostNameBack
        };
    }

    let param = {
        'cdnDomainId': id,
        'main': main,
        'back': back,
        'port': port,
        'originProtocol': originProtocol
    }

    let data = await sendRequest("POST", "CdnDomainSources/save", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 保存域名配置
 */
async function saveDomain(id) {
    if (!id) {
        layerWarn("加速域名ID不能为空");
        return;
    }
    let businessType = $('#businessTypeSelect').find("option:selected").val();
    let serviceArea = $('#serviceAreaSelect').find("option:selected").val();
    let data = await sendRequest("POST", "CdnDomain/save", {
        id: id,
        businessType: businessType,
        serviceArea: serviceArea,
    });
    autoLayer(data);
    setTimeout(function() {
        reload()
    }, 1000);
}

/**
 * 显示操作日志面板
 */
async function showOperationLog(userId) {
    if (!userId) {
        layerWarn("用户ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "OperationLog/queryOperationLogs", {
        userId: userId
    });
    if (data['code'] === 'SUCCESS') {
        $('#operationLogUl').html('');
        let userImg = $('#loginUserImg').attr('src');
        let userName = $('#loginUserName').text();
        for (let i = 0; i < data['data'].length; i++) {
            let cur = data['data'][i];
            let li = "            <li class=\"step-item\">\n" + "                <div class=\"step-content-wrapper\">\n" + "                    <div class=\"step-avatar\">\n" + "                        <img class=\"step-avatar\" src=\"" + userImg + "\"\n" + "                             alt=\"Image Description\">\n" + "                    </div>\n" + "                    <div class=\"step-content\">\n" + "                        <h5 class=\"mb-1\">" + userName + "</h5>\n" + "                        <p class=\"fs-5 mb-1\">" + cur.opDescribe + "</p>\n" + "                        <span class=\"small text-muted text-uppercase\">" + cur.createTime + "</span>\n" + "                    </div>\n" + "                </div>\n" + "            </li>"
            $('#operationLogUl').append(li);
        }
        $('#offcanvasActivityStream').addClass('show');
    }
}

/**
 * 隐藏操作日志面板
 */
function hideOperationLog() {
    $('#offcanvasActivityStream').removeClass('show')
}

/**
 * 批量删除工单分类
 */
async function deleteWorkOrderTypeBatch() {
    let batchChecks = $('input[name="batchCheck"]:checkbox:checked');
    let ids = []
    $.each(batchChecks, function() {
        ids.push($(this).val())
    });
    if (!ids) {
        layerWarn("未选中任何数据");
        return;
    }
    let data = await sendRequest("POST", "WorkOrderType/deleteBatch", {
        ids: ids
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 删除工单分类
 * @param id
 */
async function deleteWorkOrderType(id) {
    if (!id) {
        layerWarn("分类ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "WorkOrderType/delete", {
        id: id
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 添加工单分类
 * @param id
 */
async function addWorkOrderType() {
    let typeName = $('#typeName').val();
    let typeRemark = $('#typeRemark').val();
    if (!typeName) {
        layerWarn("分类名称不能为空");
        return;
    }
    if (!typeRemark) {
        layerWarn("描述信息不能为空");
        return;
    }
    let data = await sendRequest("POST", "WorkOrderType/save", {
        typeName: typeName,
        remark: typeRemark,
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 更新工单分类
 * @param id
 */
async function updateWorkOrderType() {
    let typeId = $('#typeId').val();
    let typeName = $('#typeNameUpdate').val();
    let typeRemark = $('#typeRemarkUpdate').val();
    if (!typeId) {
        layerWarn("分类ID不能为空");
        return;
    }
    if (!typeName) {
        layerWarn("分类名称不能为空");
        return;
    }
    if (!typeRemark) {
        layerWarn("描述信息不能为空");
        return;
    }
    let data = await sendRequest("POST", "WorkOrderType/save", {
        id: typeId,
        typeName: typeName,
        remark: typeRemark,
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 开启修改
 */
function openWorkOrderTypeUpdateModal(dataStr) {
    let data = JSON.parse(dataStr);
    $('#typeId').val(data.id);
    $('#typeNameUpdate').val(data.typeName);
    $('#typeRemarkUpdate').val(data.remark);
    turnModal('exampleModalCenter1', 'on');
}

/**
 * 添加角色
 */
async function addSysRole() {
    let roleName = $('#roleName').val();
    let roleCode = $('#roleCode').val();
    let roleRemark = $('#roleRemark').val();
    if (!roleName) {
        layerWarn("角色名称不能为空");
        return;
    }
    if (!roleCode) {
        layerWarn("角色编码不能为空");
        return;
    }
    if (!roleRemark) {
        layerWarn("描述信息不能为空");
        return;
    }
    let data = await sendRequest("POST", "SysRole/save", {
        roleCode: roleCode,
        roleName: roleName,
        remark: roleRemark,
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 更新角色
 * @param id
 */
async function updateSysRole() {
    let roleId = $('#roleId').val();
    let roleName = $('#roleNameUpdate').val();
    let roleCode = $('#roleCodeUpdate').val();
    let roleRemark = $('#roleRemarkUpdate').val();
    if (!roleId) {
        layerWarn("角色ID不能为空");
        return;
    }
    if (!roleName) {
        layerWarn("角色名称不能为空");
        return;
    }
    if (!roleRemark) {
        layerWarn("描述信息不能为空");
        return;
    }
    let data = await sendRequest("POST", "SysRole/save", {
        id: roleId,
        roleName: roleName,
        roleCode: roleCode,
        remark: roleRemark,
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 开启修改
 */
function openSysRoleUpdateModal(dataStr) {
    let data = JSON.parse(dataStr);
    $('#roleId').val(data.id);
    $('#roleNameUpdate').val(data.roleName);
    $('#roleCodeUpdate').val(data.roleCode);
    $('#roleRemarkUpdate').val(data.remark);
    turnModal('exampleModalCenter1', 'on');
}

/**
 * 开启修改
 */
function openAuthenticationDetailModal(dataStr) {
    const data = JSON.parse(dataStr);
    console.log(data);
    $('#name').val(data.name);
    $('#idCardNum').val(data.idCardNum);
    $('#remark').val(data.remark);
    $('#roleCodeUpdate').val(data.roleCode);
    $('#roleRemarkUpdate').val(data.remark);
    $('#frontImg').attr('src', data.frontImg);
    if ('enterprise' === data.authenticationType) {
        $('#backImgAlert').hide();
        $('#authFileDiv').show();
        $('#imgLabel').text('营业执照正本或副本照片');
        $('#authFile').attr('href', data.backImg);
    } else {
        $('#backImgAlert').show();
        $('#authFileDiv').hide();
        $('#imgLabel').text('正面照片');
        $('#backImg').attr('src', data.backImg);
    }
    turnModal('exampleModalCenter1', 'on');
}

/**
 * 删除角色
 * @param id
 */
async function deleteSysRole(id) {
    if (!id) {
        layerWarn("角色ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "SysRole/delete", {
        id: id
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 批量删除角色
 */
async function deleteRoleBatch() {
    let batchChecks = $('input[name="batchCheck"]:checkbox:checked');
    let ids = []
    $.each(batchChecks, function() {
        ids.push($(this).val())
    });
    if (!ids) {
        layerWarn("未选中任何数据");
        return;
    }
    let data = await sendRequest("POST", "SysRole/deleteBatch", {
        ids: ids
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 开启修改
 */
function openUserFlowPriceUpdateModal(dataStr) {
    let data = JSON.parse(dataStr);
    $('#userId').val(data.user.id);
    $('#userName').val(data.user.userName);
    $('#flowPrice').val(data.user.flowPrice);
    $('#maxDomainCount').val(data.user.maxDomainCount);
    const routeSelect = document.getElementById('route');
    const routeTomSelect = routeSelect && routeSelect.tomselect ? routeSelect.tomselect : null;
    if (routeTomSelect) {
        routeTomSelect.removeOption('self_hosted');
        if (data.user.route === 'self_hosted') {
            routeTomSelect.addOption({
                value: 'self_hosted',
                text: '自建 CDN（旧版兼容）'
            });
        }
        if (routeTomSelect.options[data.user.route]) {
            routeTomSelect.setValue(data.user.route, true);
        } else {
            routeTomSelect.setValue(Object.keys(routeTomSelect.options)[0] || '', true);
        }
    } else {
        $('#route option[value="self_hosted"]').remove();
        if (data.user.route === 'self_hosted') {
            $('#route').append('<option value="self_hosted">自建 CDN（旧版兼容）</option>');
        }
        if ($('#route option[value="' + data.user.route + '"]').length > 0) {
            $('#route').val(data.user.route);
        } else {
            $('#route').val($('#route option:first').val());
        }
    }
    // 赋值
    if (data.user.enableOverseas && data.user.enableOverseas == 1) {
        $('#enableOverseas').prop("checked", true);
    } else {
        $('#disableOverseas').prop("checked", true);
    }
    if (data.user.enableGlobal && data.user.enableGlobal == 1) {
        $('#enableGlobal').prop("checked", true);
    } else {
        $('#disableGlobal').prop("checked", true);
    }
    turnModal('exampleModalCenter1', 'on');
}

/**
 * 修改用户流量单价
 */
async function updateUserPrice() {
    let userId = $('#userId').val();
    let flowPrice = $('#flowPrice').val();
    let maxDomainCount = $('#maxDomainCount').val();
    let route = $('#route').find("option:selected").val();
    const enableOverseas = $('input[name="enableOverseas"]:checked').val();
    const enableGlobal = $('input[name="enableGlobal"]:checked').val();
    if (!userId) {
        layerWarn("用户ID不能为空");
        return;
    }
    if (!flowPrice) {
        layerWarn("流量单价不能为空");
        return;
    }
    if (!maxDomainCount) {
        layerWarn("域名额度不能为空");
        return;
    }
    let data = await sendRequest("POST", "SysUser/updateUserFlowPrice", {
        userId: userId,
        flowPrice: flowPrice,
        maxDomainCount: maxDomainCount,
        route: route,
        enableOverseas: enableOverseas,
        enableGlobal: enableGlobal
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 查询我的资源消耗统计
 * @param startTime
 * @param endTime
 * @returns {Promise<*>}
 */
async function queryMyResourceStatistics(startTime, endTime) {
    let data = await sendRequest("GET", "CdnDomainStatistics/queryMyResourceStatistics", {
        startTime: startTime,
        endTime: endTime
    });
    if (!data) {
        layerFail('请求失败');
    } else {
        if (data['code'] === 'SUCCESS') {
            // 适配新的数据结构，从Resource键中提取数据
            let result = data.data;
            if (result && result.Resource) {
                // 添加labels字段
                result.Resource.labels = result.labels;
                return result.Resource;
            }
            return result;
        } else {
            // layerFail(data['message']);
        }
    }
}

/**
 * 查询资源统计
 * @param domainId
 * @param startTime
 * @param endTime
 * @returns {Promise<*>}
 */
async function queryResourceStatistics(domainId, startTime, endTime) {
    let data = await sendRequest("GET", "CdnDomainStatistics/queryResourceStatistics", {
        domainId: domainId,
        startTime: startTime,
        endTime: endTime,
    });
    if (!data) {
        layerFail('请求失败');
    } else {
        if (data['code'] === 'SUCCESS') {
            // 适配新的数据结构，从Resource键中提取数据
            let result = data.data;
            if (result && result.Resource) {
                // 添加labels字段
                result.Resource.labels = result.labels;
                return result.Resource;
            }
            return result;
        } else {
            layerFail(data['message']);
        }
    }
}

/**
 * 查询HTTP状态码统计
 * @param domainId
 * @param startTime
 * @param endTime
 * @returns {Promise<*>}
 */
async function queryHttpCodeStatusStatistics(domainId, startTime, endTime) {
    let data = await sendRequest("GET", "CdnDomainStatistics/queryHttpCodeStatusStatistics", {
        domainId: domainId,
        startTime: startTime,
        endTime: endTime,
    });
    if (!data) {
        layerFail('请求失败');
    } else {
        if (data['code'] === 'SUCCESS') {
            // 适配新的数据结构，从HttpCodeStatus键中提取数据
            let result = data.data;
            if (result && result.HttpCodeStatus) {
                // 添加labels字段
                result.HttpCodeStatus.labels = result.labels;
                return result.HttpCodeStatus;
            }
            return result;
        } else {
            layerFail(data['message']);
        }
    }
}

/**
 * 查询访问情况统计
 * @param domainId
 * @param startTime
 * @param endTime
 * @returns {Promise<*>}
 */
async function queryVisitsStatistics(domainId, startTime, endTime) {
    let data = await sendRequest("GET", "CdnDomainStatistics/queryVisitsStatistics", {
        domainId: domainId,
        startTime: startTime,
        endTime: endTime,
    });
    if (!data) {
        layerFail('请求失败');
    } else {
        if (data['code'] === 'SUCCESS') {
            // 适配新的数据结构，从Visits键中提取数据
            let result = data.data;
            if (result && result.Visits) {
                // 添加labels字段
                result.Visits.labels = result.labels;
                return result.Visits;
            }
            return result;
        } else {
            layerFail(data['message']);
        }
    }
}

function changeUserDataBoard() {
    let dataUserId = $('#dataUserId').find("option:selected").val();
    if (dataUserId) {
        if (dataUserId === 'all') {
            window.location.href = 'data-board'
        } else {
            window.location.href = 'data-board?userId=' + dataUserId;
        }
    }
}

/**
 * 查询全部统计信息
 * @param startTime
 * @param endTime
 * @param domains
 * @param type
 * @returns {Promise<*>}
 */
async function queryStatistics(startTime, endTime, domains, type, userId) {
    if (!domains) {
        domains = 'all'
    }
    if (!type) {
        type = 'All'
    }
    let param = {
            startTime: startTime,
            endTime: endTime,
            domains: domains,
            type: type,
            userId
        }
        // console.log(param);
    let data = await sendRequest('GET', "CdnDomainStatistics/queryStatistics", param, 'application/x-www-form-urlencoded; charset=UTF-8', false)
    if (!data) {
        layerFail('请求失败');
    } else {
        if (data['code'] === 'SUCCESS') {
            return data.data;
        } else if (data['code'] === 'SERVER_BUSY') {
            layerFail(data['message']);
        } else {
            layerFail(data['message']);
        }
    }
}

/**
 * 获取已购流量详细明细
 * @param purchasedFlowId
 * @param startTime
 * @param endTime
 * @returns {Promise<null>}
 */
async function queryBoughtFlowDetail(purchasedFlowId, startTime, endTime) {
    let data = await sendRequest("GET", "RemovedFeature/disabled", {
        purchasedFlowId: purchasedFlowId,
        startTime: startTime,
        endTime: endTime,
    });
    if (!data) {
        layerFail('请求失败');
    } else {
        if (data['code'] === 'SUCCESS') {
            return data.data;
        } else {
            layerFail(data['message']);
        }
    }
}

/**
 * 打开订单详情弹框
 */
function openOrderModal(dataStr) {
    let data = JSON.parse(dataStr);
    $('#payType').val(data.payType === "null" ? '-' : data.payType);
    $('#createTime').val(data.createTime === "null" ? '-' : data.createTime);
    $('#payTime').val(data.payTime === "null" ? '-' : data.payTime);
    $('#detail').val(data.detail === "null" ? '-' : data.detail);
    turnModal('exampleModalCenter', 'on');
}
