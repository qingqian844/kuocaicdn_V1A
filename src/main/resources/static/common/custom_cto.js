/**
 * 修改头像
 */
async function updateImg() {
    var formdata = new FormData();
    let file = $("#editAvatarUploaderModal").get(0).files[0];
    formdata.append("file", file);
    if (!verifySuffix(file.name)) {
        layerWarn("文件格式不正确！");
        return;
    }
    let data = await sendFileUploadRequest("SysUser/updateImg", formdata);
    autoLayer(data);
    setTimeout(reload, 1000);
}

/** 用户充值*/
async function userRechargeCto(flowPackageId, buyTime) {
    let payType = $('input[name="payType"]:radio:checked').val();
    let hText = "请使用" + (payType === '2' ? "微信" : "支付宝") + "扫码进行支付";
    if (!numberReg(flowPackageId)) {
        layerWarn("请选择流量包");
        return;
    }
    let data = await sendRequest("POST", "TransactionOrder/useBalance2PayTransactionOrder", {
        flowPackageId: flowPackageId,
        buyTime: buyTime
    });
    if (data['code'] === 'SUCCESS') {
        $("#exampleModalCenteredScrollableTitle").html(hText);
        $('#qrCode').qrcode({
            render: "canvas",
            text: data['message'],
            width: "150", // 二维码的宽度
            height: "150", // 二维码的高度
            background: "#ffffff", // 二维码的后景色
            foreground: "#000000" // 二维码的前景色
        })
        setTimeout($('#testButton').click(), 1000);
        toTrackStatusOrderCTO(data['data']['id'].toString())
    } else {
        layerFail(data['message'])
        setTimeout(reload, 2000);
    }
}

/** 查询订单状态*/
async function toTrackStatusOrderCTO(orderId) {
    let timer = null;
    let x = 1;
    let data = await sendRequest("POST", "alipay/queryTransactions", {
        orderId: orderId.toString(),
    }, 'application/x-www-form-urlencoded; charset=UTF-8', loading = false);
    if (data['code'] !== 'SUCCESS') {
        timer = setTimeout(() => {
            toTrackStatusOrderCTO(orderId);
        }, 2000)
    } else {
        clearTimeout(timer);
        if (data['message'] === 'success') {
            layerSuccess("购买成功！");
            setTimeout(function() {
                window.location.href = "bought-flow-list"
            }, 1000);
        } else {
            layerFail(data['message']);
        }
    }
}


/**
 * 发送手机验证码
 */
async function sendSmsCode(id, v) {
    let userPhone = $('#' + id).val();
    if (!phoneReg(userPhone)) {
        layerWarn("请输入正确的手机号码");
        return;
    }
    let data = await sendRequest("POST", "login/sendSmsCode", Object.assign({
        userPhone: userPhone
    }, v));
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        time('#sendSmsCodeBtn', 60);
        turnModal('phoneModal', 'on');
    }
}

/**
 * 发送手机验证码_注册使用
 */
async function sendSmsCodeTemplate(o, buttonId) {
    const data = await sendRequest("POST", "login/sendSmsCodeTemplate", o);
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        time('#' + buttonId, 60);
    }
}

/**
 * 发送邮箱验证码_注册使用
 */
async function sendEmailCodeTemplate(id, buttonId, v) {
    let emailString = $('#' + id).val();
    if (!emailReg(emailString)) {
        layerWarn("请输入正确的电子邮箱");
        return;
    }
    let data = await sendRequest("POST", "login/sendEmailCodeTemplate", Object.assign({
        userEmail: emailString
    }, v));
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        time('#' + buttonId, 60);
    }
}


/**
 * 绑定手机
 */
async function setPhone() {
    let c1 = $('#phoneSrCodeInput1').val();
    let c2 = $('#phoneSrCodeInput2').val();
    let c3 = $('#phoneSrCodeInput3').val();
    let c4 = $('#phoneSrCodeInput4').val();
    if (!c1 || !c2 || !c3 || !c4) {
        return;
    }
    let code = c1 + c2 + c3 + c4
    console.log(code)
    let userPhone = $('#userPhone').val();
    if (!phoneReg(userPhone)) {
        layerWarn("请输入正确的手机号码");
        return;
    }
    let data = await sendRequest("POST", "SysUser/updatePhone", {
        phone: userPhone,
        code: code
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(reload, 1000);
    }

}

async function updateBaseInfo() {
    let autoBalance = $("#autoBalanceCheck").is(':checked');
    let userName = $('#userName').val();
    let myWebsite = $('#myWebsite').val();
    if (!userName) {
        layerWarn("用户名不可为空")
        return;
    }
    let data = await sendRequest("POST", "SysUser/updateBaseInfo", {
        userName: userName,
        myWebsite: myWebsite,
        autoBalance: autoBalance ? 1 : 0,
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(reload, 1000);
    }
}

async function sendWorkOrderMsg(id, from, type) {
    if (type === 'text') {
        let text = $('#textareaId').val();
        if (!text) {
            return;
        }
        let params = new FormData()
        params.append("msg", text)
        params.append("workOrderId", id)
        params.append("from", from)
        params.append("type", type)
        let data = await sendFileUploadRequest("WorkOrderMessage/sendMsg", params, false);
        autoLayer(data);
        if (data['code'] === 'SUCCESS') {
            $('#textareaId').value = '';
            reload();
        }
    }
    if (type === 'img') {
        const fileObj = $('#pop_file')[0].files[0];
        let params = new FormData()
        params.append("fileObj", fileObj)
        params.append("workOrderId", id)
        params.append("from", from)
        params.append("type", type)

        let data = await sendFileUploadRequest("WorkOrderMessage/sendMsg", params, false);
        autoLayer(data);
        if (data['code'] === 'SUCCESS') {
            $('#textareaId').value = '';
            reload();
        }
    }
}

async function sendWorkOrderImg(id, from, files) {
    if (!files || files.length === 0) {
        return;
    }
    const file = files[0];
    if (!verifySuffix(file.name)) {
        layerWarn("请选择图片文件哦！");
        return;
    }
    layerInfo("正在处理中，请稍后...");
    const params = new FormData()
    params.append("fileObj", file)
    params.append("workOrderId", id)
    params.append("from", from)
    params.append("type", 'img')

    const data = await sendFileUploadRequest("WorkOrderMessage/sendMsg", params, false);
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        reload();
    }
}

function enterEvent(id, from, type) {
    var event = arguments.callee.caller.arguments[0] || window.event; //调节浏览器兼容
    if (event.keyCode == 13) {
        console.log(id, from, type)
        sendWorkOrderMsg(id, from, type)
    }
}

function ctrlEnterEvent(id, from, type) {
    const event = arguments.callee.caller.arguments[0] || window.event; //调节浏览器兼容
    if (event.ctrlKey && event.keyCode === 13) {
        sendWorkOrderMsg(id, from, type)
    }
}

/**
 * 修改回源配置类型
 */

async function updateOriginType(id) {
    let originProtocol = $('input[name="originProtocol"]:radio:checked').val();

    // 在localStorage中保存当前选择的协议，以便在页面刷新后保持选中状态
    try {
        localStorage.setItem(`cdn_protocol_${id}`, originProtocol);
    } catch (e) {
        console.error("Error saving protocol:", e);
    }

    // 获取当前页面上显示的HTTP和HTTPS端口值（如果有的话）
    let httpPort = $('#httpPort').length > 0 ? $('#httpPort').val() : null;
    let httpsPort = $('#httpsPort').length > 0 ? $('#httpsPort').val() : null;

    // 如果页面上没有端口输入框，尝试从localStorage获取
    if (!httpPort || !httpsPort) {
        try {
            const savedPorts = JSON.parse(localStorage.getItem(`cdn_ports_${id}`));
            if (savedPorts) {
                httpPort = savedPorts.httpPort || 80;
                httpsPort = savedPorts.httpsPort || 443;
            } else {
                // 默认值
                httpPort = 80;
                httpsPort = 443;
            }
        } catch (e) {
            console.error("Error retrieving saved ports:", e);
            httpPort = 80;
            httpsPort = 443;
        }
    }

    // 根据选择的协议提示用户
    if (originProtocol === 'http') {
        layerInfo("已切换到HTTP回源协议，将使用HTTP端口：" + httpPort);
    } else if (originProtocol === 'https') {
        layerInfo("已切换到HTTPS回源协议，将使用HTTPS端口：" + httpsPort);
    } else if (originProtocol === 'follow') {
        layerInfo("已切换到协议跟随模式，将根据请求协议自动选择端口");
    }

    let param = {
        "doMainId": id,
        "originProtocol": originProtocol,
        "httpPort": httpPort,
        "httpsPort": httpsPort
    }

    console.log("Updating origin protocol with ports:", param);

    let data = await sendRequest("POST", "CdnDomainOriginHost/saveOriginProtocol", JSON.stringify(param), "application/json");
    autoLayer(data);

    setTimeout(function() {
        reload()
    }, 1000);
}

/**
 * 修改回源URL改写
 * type = 1 新增
 * type = 2 修改
 * type = 3 删除
 */
async function saveOriginRequestUrlRewrite(id, sources, type, index) {
    sources = JSON.parse(sources) || []
    if (type == 1) {
        let source = {
            "match_type": $("#inputGroupMergeGenderSelect option:selected").val(),
            "source_url": $("#rewrittenBackToTheSourceUrl").val(),
            "target_url": $("#targetReturnSourceURL").val(),
            "priority": $("#priority").val()
        }
        sources.push(source)
    }
    if (type == 2) {
        let hiddenIndex = $('#hiddenIndex').val();
        source = sources[hiddenIndex]
        source.match_type = $("#inputGroupMergeGenderSelect1 option:selected").val()
        source.source_url = $("#rewrittenBackToTheSourceUrl1").val()
        source.target_url = $("#targetReturnSourceURL1").val()
        source.priority = $("#priority1").val()
    }
    if (type == 3) {
        sources.splice(index, 1)
    }
    let param = {
        "doMainId": id,
        "originRequestUrlRewriteDTOS": sources
    }
    let data = await sendRequest("POST", "CdnDomainOriginHost/saveOriginRequestUrlRewrite", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}


/**
 * 修改高级回源
 * type = 1 新增
 * type = 2 修改
 * type = 3 删除
 */
async function saveAdvancedReturnSource(id, sources, type, index) {
    sources = JSON.parse(sources) || []
    if (type == 1) {
        let source = {
            "match_type": $("#uriMatchingModeAdvancedSelect option:selected").val(),
            "match_pattern": $("#uriMatchingRuleAdvanced").val(),
            "back_sources": [{
                "sources_type": $("#sourceStationTypeAdvancedSelect option:selected").val(),
                "ip_or_domain": $("#addressAdvanced").val()
            }],
            "priority": $("#priorityAdvanced").val()
        }
        sources.push(source)
    }
    if (type == 2) {
        let hiddenIndex = $('#hiddenIndexAdvanced').val();
        let source = {
            "match_type": $("#uriMatchingModeAdvancedSelect1 option:selected").val(),
            "match_pattern": $("#uriMatchingRuleAdvanced1").val(),
            "back_sources": [{
                "sources_type": $("#sourceStationTypeAdvancedSelect1 option:selected").val(),
                "ip_or_domain": $("#addressAdvanced1").val()
            }],
            "priority": $("#priorityAdvanced1").val()
        }

        sources[hiddenIndex] = source
    }
    if (type == 3) {
        sources.splice(index, 1)
    }
    console.log(sources)
    let param = {
        "doMainId": id,
        "flexibleOrigins": sources
    }
    let data = await sendRequest("POST", "CdnDomainOriginHost/saveAdvancedReturnSource", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 修改Range回源
 */

async function saveRangeSwitch(id, status) {
    let param = {
        "doMainId": id,
        "status": status
    }
    let data = await sendRequest("POST", "CdnDomainOriginHost/saveRangeSwitch", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {

    }
    setTimeout(function() {
        reload()
    }, 1000);
}

/**
 * 回源是否校验ETag
 */
async function saveRangeVerifyETag(id, status) {
    let param = {
        "doMainId": id,
        "status": status
    }
    let data = await sendRequest("POST", "CdnDomainOriginHost/saveRangeVerifyETag", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {

    }
    setTimeout(function() {
        reload()
    }, 1000);
}

/**
 * 回源是否校验ETag
 */
async function saveRangeTimeOut(id) {
    let timeout = $('#exampleFormControlInput1').val();
    if (timeout < 5 || timeout > 60) {
        layerWarn("回源超时时间在5-60秒范围！")
        errorShake('exampleFormControlInput1')
        return;
    }
    let param = {
        "doMainId": id,
        "originReceiveTimeOut": timeout
    }
    let data = await sendRequest("POST", "CdnDomainOriginHost/saveRangeTimeOut", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {

    }
    setTimeout(function() {
        reload()
    }, 1000);
}

/**
 * 修改回源请求头
 * type = 1 新增
 * type = 2 修改
 * type = 3 删除
 */
async function saveOriginRequestHeader(id, sources, type, index) {
    sources = JSON.parse(sources) || []
    if (type == 1) {
        name = $("#requestHeaderParameter").val()
        value = $("#requestHeaderValue").val()
        if (!name) {
            layerWarn("请填写请求头参数!")
            errorShake('requestHeaderParameter')
            return;
        }
        const exists = sources.some(source => source.name === name);

        if (exists) {
            layerWarn("该请求头参数已存在!")
            errorShake('requestHeaderParameter')
            return;
        }
        if (!value) {
            layerWarn("请填写请求头参数值!")
            errorShake('requestHeaderValue')
            return;
        }
        let source = {
            "name": name,
            "value": value,
            "action": $('input[name="editRadioAdd"]:radio:checked').val()
        }
        sources.push(source)
    }
    if (type == 2) {
        let name = $("#requestHeaderParameter1").val()
        let value = $("#requestHeaderValue1").val()
        let hiddenIndex = $('#hiddenHeader').val();
        if (!name) {
            layerWarn("请填写请求头参数!")
            errorShake('requestHeaderParameter1')
            return;
        }
        let source = {
                "name": name,
                "value": value,
                "action": $('input[name="editRadioUpdate"]:radio:checked').val()
            }
            // 定义一个变量来存储是否找到了匹配的 name
        let found = false;
        // 迭代 sources 数组
        for (let i = 0; i < sources.length; i++) {
            // 判断当前元素的 name 是否等于目标名字
            if (sources[i].name === source.name && i != hiddenIndex) {
                // 如果是，并且这个 name 不是下标为 1 的元素，则将 found 设为 true
                found = true;
                break;
            }
        }
        if (found) {
            // 找到了匹配的 name
            layerWarn("该请求头参数已存在!")
            errorShake('requestHeaderParameter1')
            return;
        }

        sources[hiddenIndex] = source
    }
    if (type == 3) {
        sources.splice(index, 1)
    }
    let param = {
        "doMainId": id,
        "originRequestHeader": sources
    }
    let data = await sendRequest("POST", "CdnDomainOriginHost/saveOriginRequestHeader", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}


/**
 * HTTPS配置
 */

async function httpsConfiguration(id) {
    let param = {}
    originRangeStatus = document.getElementById("origin_range_status")
    if (originRangeStatus.checked) {
        let certSource = $("#exampleFormControlSelect1 option:checked").val()
        let certName = $("#exampleFormControlInput1").val()
        let certValue = $("#exampleFormControlTextarea1").val()
        let privateKey = $("#exampleFormControlTextarea2").val()
        if (!certSource) {
            layerWarn("证书信息不完整!")
            return;
        }
        if (!certName) {
            layerWarn("证书信息不完整!")
            errorShake('exampleFormControlInput1')
            return;
        }
        if (!certNameReg(certName)) {
            layerWarn("输入长度范围为3-64个字符");
            errorShake('exampleFormControlInput1')
            return;
        }
        if (!certValue) {
            layerWarn("证书信息不完整!")
            errorShake('exampleFormControlTextarea1')
            return;
        }
        if (!privateKey) {
            layerWarn("证书信息不完整!")
            errorShake('exampleFormControlTextarea2')
            return;
        }
        if (!certValue.includes("-----BEGIN CERTIFICATE-----") || !certValue.includes("-----END CERTIFICATE-----")) {
            layerWarn("证书内容格式不正确，请填写 PEM 格式证书");
            errorShake('exampleFormControlTextarea1')
            return;
        }
        if ((!privateKey.includes("-----BEGIN PRIVATE KEY-----") && !privateKey.includes("-----BEGIN RSA PRIVATE KEY-----"))
            || (!privateKey.includes("-----END PRIVATE KEY-----") && !privateKey.includes("-----END RSA PRIVATE KEY-----"))) {
            layerWarn("私钥内容格式不正确，请填写 PEM 格式私钥");
            errorShake('exampleFormControlTextarea2')
            return;
        }
        param = {
            "doMainId": id,
            "https": {
                "https_status": "on",
                "certificate_source": certSource,
                "certificate_name": certName,
                "certificate_value": certValue,
                "private_key": privateKey
            }

        }
    } else {
        param = {
            "doMainId": id,
            "https": {
                "https_status": "off"
            }
        }
    }


    let data = await sendRequest("POST", "CdnDomainHttps/httpsConfiguration", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * TLS版本配置
 */
async function tlsVersionConfiguration(id) {
    let tls_version = "";
    $("input[name = 'tlsVersionConfigurationInput']:checked").each(function() {
        console.log(this.value)
        if (tls_version) {
            tls_version = tls_version + "," + this.value
        } else {
            tls_version = tls_version + this.value
        }
    })
    if (!tls_version) {
        layerWarn("请至少选择一个 TLS 版本");
        return;
    }
    let param = {
        "doMainId": id,
        "https": {
            "tls_version": tls_version,
        }
    }

    let data = await sendRequest("POST", "CdnDomainHttps/httpsConfigurationOther", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}


/**
 * 强制跳转
 */

/**
 * 切换强制跳转代码选择框的显示状态
 */
function toggleRedirectCodeSelect() {
    const isChecked = $("#force_redirect_status").is(':checked');
    if (isChecked) {
        $("#forcedToJumpCode").show();
        // 重新初始化Tom Select组件
        setTimeout(function() {
            if (HSCore && HSCore.components && HSCore.components.HSTomSelect) {
                // 先销毁可能已存在的实例
                const tomSelectInstance = document.querySelector('#forcedToJumpCodeSelect')._tomSelect;
                if (tomSelectInstance) {
                    tomSelectInstance.destroy();
                }
                // 重新初始化
                HSCore.components.HSTomSelect.init('#forcedToJumpCodeSelect');
            }
        }, 100);
    } else {
        $("#forcedToJumpCode").hide();
    }
}

/**
 * 强制跳转
 */
async function forcedToJump(id) {
    let param = {};
    let status = $("#force_redirect_status").is(':checked') ? 'on' : 'off';
    let redirectCode = $("#forcedToJumpCodeSelect").val() || '302'; // 默认使用302

    // 检查HTTPS是否已开启
    if (status === 'on' && $("#origin_range_status").length > 0 && !$("#origin_range_status").is(':checked')) {
        layerWarn("请先配置并开启HTTPS证书后再启用强制跳转");
        // 恢复开关状态
        $("#force_redirect_status").prop("checked", false);
        $("#forcedToJumpCode").hide();
        return;
    }

    if (status === 'on') {
        param = {
            "doMainId": id,
            "forceRedirect": {
                "status": status,
                "redirect_code": parseInt(redirectCode)
            }
        };
    } else {
        param = {
            "doMainId": id,
            "forceRedirect": {
                "status": status
            }
        };
    }

    let data = await sendRequest("POST", "CdnDomainHttps/forcedToJump", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        // 更新UI状态
        if (status === 'on') {
            $("#force_redirect_status").prop("checked", true);
            $("#force_redirect_status").next("label").text("已开启");
            $("#forcedToJumpCode").show();

            // 更新Tom Select组件的值
            setTimeout(function() {
                if (HSCore && HSCore.components && HSCore.components.HSTomSelect) {
                    const tomSelectInstance = document.querySelector('#forcedToJumpCodeSelect')._tomSelect;
                    if (tomSelectInstance) {
                        tomSelectInstance.setValue(redirectCode);
                    }
                }
            }, 100);
        } else {
            $("#force_redirect_status").prop("checked", false);
            $("#force_redirect_status").next("label").text("已关闭");
            $("#forcedToJumpCode").hide();
        }

        setTimeout(function() {
            reload();
        }, 1000);
    } else {
        // 如果失败，也刷新页面以恢复正确的状态
        setTimeout(function() {
            reload();
        }, 1000);
    }
}

/**
 * 修改HTTP/2
 */
async function saveHttp2(id, status) {
    let param = {
        "doMainId": id,
        "https": {
            "http2_status": status
        }
    }
    let data = await sendRequest("POST", "CdnDomainHttps/httpsConfigurationOther", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    } else {
        layerFail(data.message || '操作失败，请重试');
        setTimeout(function() {
            reload()
        }, 1000);
    }
}


/**
 * OCSP Stapling
 */

async function saveOcspStapling(id, status) {
    let param = {
        "doMainId": id,
        "https": {
            "ocsp_stapling_status": status
        }
    }
    let data = await sendRequest("POST", "CdnDomainHttps/httpsConfigurationOther", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {

    }
    setTimeout(function() {
        reload()
    }, 1000);
}

/**
 * 编辑缓存规则
 * type = 1 新增
 * type = 2 修改
 * type = 3 删除
 */
async function saveCacheRulesModal(id, sources, type, index) {
    sources = JSON.parse(sources) || []
    let flag = false;
    if (type == 1) {
        // 类型
        let matchType = $("#inputRuleTypes option:selected:checked").val()
        let matchValue = $("#ruleContext").val()
        let priority = $("#priority").val()
        let cacheEnabled = $("#cacheEnableSwitch").is(":checked")
        let ttl = cacheEnabled ? $("#expiseTimeNumber").val() : null
        let ttlUnit = $("#expiseTime option:selected:checked").val()
        let urlParameterType = $("#urlParam option:selected:checked").val()
        let urlParameterValue = $("#urlParamValue").val()
        console.log(matchValue)
        if (matchType == "file_extension") {
            if (!fileSuffixReg(matchValue)) {
                layerFail("文件扩展名格式不正确！请使用英文逗号分隔，如：.zip,.jpg")
                errorShake("ruleContext")
                return 0;
            }
        } else if (matchType == "catalog") {
            if (!dirpathReg(matchValue) && matchValue != "/") {
                layerFail("目录路径输入不合法")
                errorShake("ruleContext")
                return 0;
            }
        } else if (matchType == "full_path") {
            if (!fullDirPathRef(matchValue)) {
                layerFail("全路径输入不合法！")
                errorShake("ruleContext")
                return 0;
            }
        }
        // if(parseInt(priority)< 1 || parseInt(priority) > 99){
        //     layerFail("优先级输入不合法")
        //     errorShake("priority")
        //     return 0;
        // }
        // sources.forEach(item => {
        //     if (item.priority == priority) {
        //         layerFail("优先级不能重复")
        //         errorShake("priority")
        //         flag = true;
        //         return 0;
        //     }
        // });
        if (flag) {
            return;
        }
        if (cacheEnabled && !ttl) {
            layerFail("缓存失效时间不能为空")
            errorShake("expiseTimeNumber")
            return 0;
        }
        // 新增的参数
        let source = {
            "match_type": matchType,
            "match_value": matchValue,
            "ttl": ttl,
            "ttl_unit": ttlUnit,
            "priority": priority,
            "url_parameter_type": urlParameterType,
            "url_parameter_value": urlParameterValue,
            "follow_origin": $("#followOrigin").is(":checked") ? "on" : "off"
        }
        sources.push(source)
    }
    if (type == 2) {
        let hiddenIndex = $('#hiddenIndex').val();
        let matchType = $("#inputRuleTypes1 option:selected:checked").val()
        let matchValue = $("#ruleContext1").val()
        let priority = $("#priority1").val()
        let cacheEnabled = $("#cacheEnableSwitch1").is(":checked")
        let ttl = cacheEnabled ? $("#expiseTimeNumber1").val() : null
        if (matchType == "file_extension") {
            if (!fileSuffixReg(matchValue)) {
                layerFail("文件扩展名格式不正确！请使用英文逗号分隔，如：.zip,.jpg")
                errorShake("ruleContext1")
                return 0;
            }
        } else if (matchType == "catalog") {
            if (!dirpathReg(matchValue) && matchValue != "/") {
                layerFail("目录路径输入不合法")
                errorShake("ruleContext1")
                return 0;
            }
        } else if (matchType == "full_path") {
            if (!fullDirPathRef(matchValue)) {
                layerFail("全路径输入不合法！")
                errorShake("ruleContext1")
                return 0;
            }
        }
        // if(parseInt(priority)< 1 || parseInt(priority) > 99){
        //     layerFail("优先级输入不合法")
        //     errorShake("priority1")
        //     return 0;
        // }
        // for (let i = 0; i < sources.length; i++) {
        //     if (hiddenIndex != i) {
        //         if (sources[i].priority == priority) {
        //             layerFail("优先级不能重复")
        //             errorShake("priority1")
        //             flag = true;
        //             return;
        //         }
        //     }
        // }
        if (flag) {
            return;
        }
        if (cacheEnabled && !ttl) {
            layerFail("缓存失效时间不能为空")
            errorShake("expiseTimeNumber1")
            return 0;
        }
        source = sources[hiddenIndex]
        source.match_type = matchType
        source.match_value = matchValue
        source.priority = priority
        source.ttl = ttl
        source.ttl_unit = $("#expiseTime1 option:selected:checked").val()
        source.url_parameter_type = $("#urlParam1 option:selected:checked").val()
        source.url_parameter_value = $("#urlParamValue1").val()
        source.follow_origin = $("#followOrigin1").is(":checked") ? "on" : "off"
    }
    if (type == 3) {
        sourceItem = sources[index]
            /** if (sourceItem.match_type == "all"){
                layerFail("该缓存规则不能删除")
                return
            } **/
        sources.splice(index, 1)
    }
    console.log(sources)
    let param = {
        "doMainId": id,
        "cacheRules": sources
    }
    let data = await sendRequest("POST", "CdnDomainCache/saveCacheRules", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 移动缓存规则
 */
async function moveCacheRules(id, sources) {
    if (!sources) {
        layerFail("缓存规则不能为空")
        return
    }
    let param = {
        "doMainId": id,
        "cacheRules": sources
    }
    console.log(id, sources)
        // return
    let data = await sendRequest("POST", "CdnDomainCache/saveCacheRules", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}





/**
 * 缓存遵循源站状态
 */

async function saveCacheFollowOriginStatusSwitch(id, status) {
    if (!id || !status) {
        layerFail("参数错误")
    }
    let param = {
        "doMainId": id,
        "cacheFollowOriginStatus": status
    }
    let data = await sendRequest("POST", "CdnDomainCache/saveCacheFollowOriginStatusSwitch", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {

    }
    setTimeout(function() {
        reload()
    }, 1000);
}

function toSecond(num, unit) {
    if (unit == 's') {
        return num;
    } else if (unit == 'm') {
        return num * 60;
    } else if (unit == 'h') {
        return num * 60 * 60;
    } else if (unit == 'd') {
        return num * 60 * 60 * 24;
    }
}

/**
 * 保存状态码缓存时间
 * type = 1 新增
 * type = 2 修改
 * type = 3 删除
 */
async function saveErrorCodeCache(id, sources, type, index) {
    sources = JSON.parse(sources) || []
    let flag = false;
    let errorCodes = [400, 403, 404, 405, 414, 500, 501, 502, 503, 504];
    if (type == 1) {
        let errorCode = $("#statusCode").val()
        let errCodeExpireTimeNumber = $("#errCodeExpireTimeNumber").val()
        let errCodeExpireTime = $("#errCodeExpireTime option:checked").val()
        let ttl = toSecond(errCodeExpireTimeNumber, errCodeExpireTime)
        if (!errorCodes.includes(parseInt(errorCode))) {
            layerFail("不支持该状态码!")
            errorShake("statusCode")
            return 0;
        }
        for (let i = 0; i < sources.length; i++) {
            if (sources[i].code == errorCode) {
                layerFail("该状态码重复!")
                errorShake("statusCode")
                return 0;
            }
        }
        if (!errCodeExpireTimeNumber) {
            layerFail("缓存时间不能为空!")
            errorShake("errCodeExpireTimeNumber")
            return 0;
        }
        // 新增的参数
        let source = {
            "code": errorCode,
            "ttl": ttl
        }
        sources.push(source)
    }
    if (type == 2) {
        let hiddenIndex = $('#hiddenIndex').val();
        let errorCode = $("#statusCode1").val()
        let errCodeExpireTimeNumber = $("#errCodeExpireTimeNumber1").val()
        let errCodeExpireTime = $("#errCodeExpireTime1 option:checked").val()
        let ttl = toSecond(errCodeExpireTimeNumber, errCodeExpireTime)
        if (!errorCodes.includes(parseInt(errorCode))) {
            layerFail("不支持该状态码!")
            errorShake("statusCode1")
            return 0;
        }
        for (let i = 0; i < sources.length; i++) {
            if (hiddenIndex != i) {
                if (sources[i].code == errorCode) {
                    layerFail("该状态码以重复")
                    errorShake("statusCode1")
                    flag = true;
                    return;
                }
            }
        }
        if (!errCodeExpireTimeNumber) {
            layerFail("缓存时间不能为空!")
            errorShake("errCodeExpireTimeNumber")
            return 0;
        }
        if (flag) {
            return 0;
        }
        source = sources[hiddenIndex]
        source.code = errorCode
        source.ttl = ttl
    }
    if (type == 3) {
        sources.splice(index, 1)
    }

    let param = {
        "doMainId": id,
        "errorCodeCache": sources
    }
    let data = await sendRequest("POST", "CdnDomainCache/saveErrorCodeCache", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 保存智能压缩
 */
async function saveCompress(id) {
    let compressType = "";
    let compressStatus = "off";
    $("input[name = 'intelligentCompression']:checked").each(function() {
        console.log(this.value)
        if (compressType) {
            compressType = compressType + "," + this.value
        } else {
            compressType = compressType + this.value
        }
    })
    if (compressType) {
        compressStatus = "on"
    }
    let param = {
        "doMainId": id,
        "compress": {
            "status": compressStatus,
            "type": compressType
        }
    }
    let data = await sendRequest("POST", "CdnDomainHigher/saveCompress", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {

    }
    setTimeout(function() {
        reload()
    }, 1000);
}


/**
 * 修改HTTP header配置
 * type = 1 新增
 * type = 2 修改
 * type = 3 删除
 */
async function saveHttpHeader(id, sources, type, index) {
    sources = JSON.parse(sources) || []
    if (type == 1) {
        let checkedValue = $("#responseHeaderParameterSelect option:checked").val()
        let name = ""
        if (checkedValue === "customize") {
            name = $("#responseHeaderParameterCustom").val()
        } else {
            name = checkedValue
        }
        let action = $('input[name="editUserModalAccountTypeModalRadioEg1_"]:radio:checked').val()
        for (let i = 0; i < sources.length; i++) {
            let item = sources[i]
            if (name === item.name && action === item.action) {
                layerWarn("响应头参数已存在")
                return
            }
        }
        let source = {
            "name": name,
            "value": $("#responseHeaderValue").val(),
            "action": action
        }
        sources.push(source)
    }
    if (type == 2) {
        let hiddenIndex = $('#hiddenHeader').val();
        let checkedValue = $("#responseHeaderParameterSelect1 option:checked").val()
        let name = ""
        if (checkedValue === "customize") {
            name = $("#responseHeaderParameterCustom").val()
        } else {
            name = checkedValue
        }
        let action = $('input[name="editUserModalAccountTypeModalRadioEg2_"]:radio:checked').val()
        console.log(hiddenIndex)
        for (let i = 0; i < sources.length; i++) {
            if (i == hiddenIndex) {
                continue
            }
            let item = sources[i]
            if (name == item.name && action == item.action) {
                layerWarn("响应头参数已存在")
                return
            }
        }
        let source = {
            "name": name,
            "value": $("#responseHeaderValue1").val(),
            "action": action
        }

        sources[hiddenIndex] = source
    }
    if (type == 3) {
        sources.splice(index, 1)
    }
    let param = {
        "doMainId": id,
        "httpResponseHeaders": sources
    }

    let data = await sendRequest("POST", "CdnDomainHigher/saveHttpHeader", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 修改自定义错误页面配置
 * type = 1 新增
 * type = 2 修改
 * type = 3 删除
 */
async function saveCustomErrorPageConfiguration(id, sources, type, index) {
    let codes = ["400", "403", "404", "405", "414", "416", "451", "500", "501", "502", "503", "504"]
    sources = JSON.parse(sources) || []
    if (type == 1) {
        let errorCode = $("#errorCode").val()
        if (!codes.includes(errorCode)) {
            layerWarn("请检查错误码是否正确！")
            reutrn;
        }
        for (let i = 0; i < sources.length; i++) {
            let item = sources[i]
            if (errorCode == item.error_code) {
                layerWarn("错误码已经存在！")
                return
            }
        }
        let reChangeUrl = $("#reChangeUrl").val();
        if (!verifyUrlPrefix(reChangeUrl)) {
            layerWarn("请检查重定向路径！")
            return;
        }
        let source = {
            "error_code": errorCode,
            "target_link": reChangeUrl,
            "target_code": $('input[name="reChangeCustomErrorPageInputName1"]:radio:checked').val()
        }
        sources.push(source)
    }
    if (type == 2) {
        let hiddenIndex = $('#hiddenIndex').val();
        let errorCode = $("#errorCode1").val()
        if (!codes.includes(errorCode)) {
            layerWarn("请检查重定向路径是否正确！")
            reutrn;
        }
        for (let i = 0; i < sources.length; i++) {
            let item = sources[i]
            if (hiddenIndex === i) {
                continue
            }
            if (errorCode === item.error_code) {
                layerWarn("错误码已经存在！")
                return
            }
        }
        let reChangeUrl = $("#reChangeUrl1").val()
        if (!verifyUrlPrefix(reChangeUrl)) {
            layerWarn("请检查重定向路径是否正确！")
            return;
        }
        let source = {
            "error_code": errorCode,
            "target_link": reChangeUrl,
            "target_code": $('input[name="reChangeCustomErrorPageInputName11"]:radio:checked').val()
        }

        sources[hiddenIndex] = source
    }
    if (type == 3) {
        sources.splice(index, 1)
    }
    let param = {
        "doMainId": id,
        "errorCodeRedirectRules": sources
    }
    let data = await sendRequest("POST", "CdnDomainHigher/saveCustomErrorPageConfiguration", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 保存防盗链信息
 */
async function saveReferer(id) {
    //是否开启防盗链
    let status = $("#refererStatus").val()
        // 空referer
    let includeEmpty = $("#includeEmpty").val() == 'on' ? 'true' : 'false'
        //规则信息
    let refererListType = $("#refererListType").val()
        //关闭是0
    let refererType = 0
    let referers = []
    if (status == 'on') {
        refererType = $('input[name = "refererTypeSwitchName"]:radio:checked').val()
        if (!refererListType) {
            layerWarn("请填写规则信息！")
            return;
        }
        referers = refererListType.split(/\r?\n/)
        if (referers.length > 400) {
            layerWarn("最多只能添加400个规则信息!")
            errorShake("refererListType")
            return
        }
        for (let referersKey in referers) {
            console.log(referers[referersKey])
            if (!antiLeechLinkReg(referers[referersKey])) {
                layerWarn("请填写正确的规则信息！")
                errorShake("refererListType")
                return;
            }
        }
    }
    let param = {
        "doMainId": id,
        "referer": {
            "referer_type": refererType,
            "referers": referers,
            "include_empty": includeEmpty
        }
    }
    let data = await sendRequest("POST", "CdnDomainAccess/saveHotlinkPrevention", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }

}

/**
 * 保存IP黑白名单信息
 */
async function saveIpBlackWhiteList(id) {
    //是否开启防盗链
    let status = $("#ipAclStatus").val()
        //规则信息
    let ips = $("#ipAclText").val()
    let ipList = []

    //关闭是0
    let ipaclType = 0
    if (status == 'on') {
        ipaclType = $('input[name = "ipAclTypeSwitchName"]:radio:checked').val()
        if (!ips) {
            layerWarn("请填写规则信息！")
            return;
        }
        ipList = ips.split(/\r?\n/)
        if (ipList.length > 150) {
            layerWarn("最多只能添加150个规则信息!")
            errorShake("ipAclText")
            return
        }
        for (let referersKey in ipList) {
            console.log(ipList[referersKey])
            if (!ipBlackWhiteReg(ipList[referersKey])) {
                layerWarn("请填写正确的规则信息！")
                errorShake("ipAclText")
                return;
            }
        }
    }
    let param = {
        "doMainId": id,
        "type": ipaclType,
        "ips": ipList
    }
    let data = await sendRequest("POST", "CdnDomainAccess/saveIpBlackWhiteList", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }

}

/**
 * 保存User-Agent黑白名单信息
 */
async function saveUserAgentFilter(id) {
    //是否开启防盗链
    let status = $("#userAgentFilterStatus").val()
        //规则信息
    let userAgentFilterValue = $("#userAgentFilterValue").val()
        //关闭是0
    let userAgentFilterStatus = 0
    let usList = []
    if (status == 'on') {
        userAgentFilterStatus = $('input[name = "userAgentFilterTypeName"]:radio:checked').val()
        if (!userAgentFilterValue) {
            layerWarn("请填写规则信息！")
            errorShake("userAgentFilterValue")
            return;
        }
        usList = userAgentFilterValue.split(/\r?\n/)
        if (usList.length > 10) {
            layerWarn("最多只能添加10个规则信息!")
            errorShake("userAgentFilterValue")
            return
        }
        for (let referersKey in usList) {
            console.log(usList[referersKey])
            if (!uaHuaWeiReg(usList[referersKey]) || !uaVolReg(usList[referersKey])) {
                layerWarn("请填写正确的规则信息！")
                errorShake("userAgentFilterValue")
                return;
            }
        }
    }
    let param = {
        "doMainId": id,
        "userAgentBlackAndWhiteListDTO": {
            "type": userAgentFilterStatus,
            "ua_list": usList
        }
    }
    let data = await sendRequest("POST", "CdnDomainAccess/saveUserAgentFilter", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }

}

/**
 * 新增流量包
 */
async function saveEdgeOneSecurityPolicy(id) {
    const rateLimitEnabled = $("#eoRateLimitEnabled").is(":checked") ? "on" : "off";
    const exceptionEnabled = $("#eoExceptionEnabled").is(":checked") ? "on" : "off";
    const threshold = Number($("#eoRateLimitThreshold").val() || 0);
    if (rateLimitEnabled === "on" && (!Number.isFinite(threshold) || threshold < 1 || threshold > 100000)) {
        layerWarn("速率限制阈值必须在 1 - 100000 之间");
        errorShake("eoRateLimitThreshold");
        return;
    }
    if (exceptionEnabled === "on" && !String($("#eoExceptionCondition").val() || "").trim()) {
        layerWarn("启用例外规则时必须填写匹配条件");
        errorShake("eoExceptionCondition");
        return;
    }
    const param = {
        doMainId: id,
        managedRulesEnabled: $("#eoManagedRulesEnabled").is(":checked") ? "on" : "off",
        managedRulesDetectionOnly: $("#eoManagedRulesDetectionOnly").is(":checked") ? "on" : "off",
        managedRulesSemanticAnalysis: $("#eoManagedRulesSemanticAnalysis").is(":checked") ? "on" : "off",
        managedRulesAutoUpdate: $("#eoManagedRulesAutoUpdate").is(":checked") ? "on" : "off",
        botManagementEnabled: $("#eoBotManagementEnabled").is(":checked") ? "on" : "off",
        captchaPageChallengeEnabled: $("#eoCaptchaPageChallengeEnabled").is(":checked") ? "on" : "off",
        aiCrawlerDetectionEnabled: $("#eoAiCrawlerDetectionEnabled").is(":checked") ? "on" : "off",
        aiCrawlerDetectionAction: $("#eoAiCrawlerDetectionAction").val(),
        httpDdosAdaptiveFrequencyControlEnabled: $("#eoHttpDdosAdaptiveFrequencyControlEnabled").is(":checked") ? "on" : "off",
        httpDdosAdaptiveFrequencyControlSensitivity: $("#eoHttpDdosAdaptiveFrequencyControlSensitivity").val(),
        httpDdosClientFilteringEnabled: $("#eoHttpDdosClientFilteringEnabled").is(":checked") ? "on" : "off",
        httpDdosBandwidthAbuseDefenseEnabled: $("#eoHttpDdosBandwidthAbuseDefenseEnabled").is(":checked") ? "on" : "off",
        httpDdosSlowAttackDefenseEnabled: $("#eoHttpDdosSlowAttackDefenseEnabled").is(":checked") ? "on" : "off",
        rateLimitEnabled: rateLimitEnabled,
        rateLimitThreshold: threshold || 1000,
        rateLimitPeriod: $("#eoRateLimitPeriod").val(),
        rateLimitMode: $("#eoRateLimitMode").val(),
        rateLimitAction: $("#eoRateLimitAction").val(),
        rateLimitChallengeOption: $("#eoRateLimitChallengeOption").val(),
        rateLimitActionDuration: $("#eoRateLimitActionDuration").val(),
        rateLimitCountBy: $("#eoRateLimitCountBy").val(),
        rateLimitCondition: $("#eoRateLimitCondition").val(),
        exceptionEnabled: exceptionEnabled,
        exceptionModules: $("#eoExceptionModules").val(),
        exceptionCondition: $("#eoExceptionCondition").val()
    };
    let data = await sendRequest("POST", "CdnDomainAccess/saveEdgeOneSecurityPolicy", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload();
        }, 1000);
    }
}

async function addFlowPackage() {
    let flowPackageName = $("#flowPackageName").val()
    let flowPackageRemark = $("#flowPackageRemark").val()
    let dayLimit = $("#dayLimit").val()
    let userLimit = $("#userLimit").val()
    let payType = $('input[name="payType"]:radio:checked').val()
    let packageType = $('#packageType').find("option:selected").val();
    let buyerRule = $('#buyerRule').find("option:selected").val();
    if (packageType === 'common') {
        payType = 'month'
    }
    let size = $("#size").val()
    let unit = $("#sizeType option:selected").val()
    let price = $("#price").val()
    let price3 = $("#price3").val()
    let price12 = $("#price12").val()
    let sort = $("#sort").val()
    let edgeoneDomainQuota = $("#edgeoneDomainQuota").val() || 0
    let status = $('input[name="status"]:radio:checked').val()
    if (!integerReg(sort) || sort > 100) {
        layerWarn("优先级必须为0-100的正整数！");
        return;
    }
    if (!price) {
        layerWarn("请输入流量包单价")
        return;
    }
    if (!price3) {
        price3 = 3 * price;
    }
    if (!price12) {
        price12 = 12 * price;
    }
    if (!size) {
        layerWarn("请输入流量包大小")
        return;
    }
    if (!payType) {
        layerWarn("请输入流量包类型")
        return;
    }
    // 修改原因: 数据库存储B单位
    let fSize = size;
    if (unit == "PB") {
        fSize = size * 1024 * 1024 * 1024 * 1024 * 1024;
    }
    if (unit == "TB") {
        fSize = size * 1024 * 1024 * 1024 * 1024;
    }
    if (unit == "GB") {
        fSize = size * 1024 * 1024 * 1024;
    }
    if (unit == "MB") {
        fSize = size * 1024 * 1024;
    }
    let param = {
        "packageName": flowPackageName,
        "remark": flowPackageRemark,
        "price": price,
        "price3": price3,
        "price12": price12,
        "size": fSize,
        "chargeType": payType,
        "sort": sort,
        "status": status,
        "packageType": packageType,
        "dayLimit": dayLimit,
        "userLimit": userLimit,
        "buyerRule": buyerRule,
        "edgeoneDomainQuota": edgeoneDomainQuota
    }
    let data = await sendRequest("POST", "RemovedFeature/disabled", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 修改流量包
 */
async function updateFlowPackage() {
    let id = $("#hiddenIndex").val()
    let flowPackageName = $("#flowPackageName1").val()
    let dayLimit = $("#dayLimit1").val()
    let userLimit = $("#userLimit1").val()
    let flowPackageRemark = $("#flowPackageRemark1").val()
    let payType = $('input[name="payType1"]:radio:checked').val()
    let packageType = $('#packageType1').find("option:selected").val();
    let buyerRule = $('#buyerRule1').find("option:selected").val();
    if (packageType === 'common') {
        payType = 'month'
    }
    let size = $("#size1").val()
    let unit = $("#sizeType1 option:selected").val()
    let price = $("#price1").val()
    let price3 = $("#price3Update").val()
    let price12 = $("#price12Update").val()
    let sort = $("#sort1").val()
    let edgeoneDomainQuota = $("#edgeoneDomainQuota1").val() || 0
    let status = $('input[name="status1"]:radio:checked').val()
    console.log(size)
    let fSize = size * 1024;
    console.log(fSize)
    if (!integerReg(sort) || sort > 100) {
        layerWarn("优先级必须为0-100的正整数！");
        return;
    }
    if (!price) {
        layerWarn("请输入流量包单价")
        return;
    }
    if (!price3) {
        price3 = 3 * price;
    }
    if (!price12) {
        price12 = 12 * price;
    }
    if (unit == "PB") {
        fSize = fSize * 1024 * 1024 * 1024 * 1024;
    }
    if (unit == "TB") {
        fSize = fSize * 1024 * 1024 * 1024;
    }
    if (unit == "GB") {
        fSize = fSize * 1024 * 1024;
    }
    if (unit == "MB") {
        fSize = fSize * 1024;
    }
    let param = {
        "id": id,
        "packageName": flowPackageName,
        "remark": flowPackageRemark,
        "price": price,
        "price3": price3,
        "price12": price12,
        "size": fSize,
        "chargeType": payType,
        "sort": sort,
        "status": status,
        "packageType": packageType,
        "dayLimit": dayLimit,
        "userLimit": userLimit,
        "buyerRule": buyerRule,
        "edgeoneDomainQuota": edgeoneDomainQuota
    }
    let data = await sendRequest("POST", "RemovedFeature/disabled", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 批量删除流量包
 */
async function deleteFlowPackageBatch() {
    let batchChecks = $('input[name="batchCheck"]:checkbox:checked');
    let ids = []
    $.each(batchChecks, function() {
        ids.push($(this).val())
    });
    if (!ids) {
        layerWarn("未选中任何数据");
        return;
    }
    let data = await sendRequest("POST", "RemovedFeature/disabled", {
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
 * 删除流量包
 * @param id
 */
async function deleteFlowPackage(id) {
    if (!id) {
        layerWarn("流量包ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "RemovedFeature/disabled", {
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
 * 注册用户
 */
async function registerUser(flag) {
    let recommend = getQueryString("code")
    let readme = $("#readme").is(":checked")
    if (!readme) {
        layerWarn("请勾选《服务用户协议》")
        return 0;
    }
    let phoneNum = $("#phoneNum").val()
    let phoneCode = $("#phoneCode").val()
    let userAccount = $("#userAccount").val()
    let userPassword = $("#userPassword1").val()
    if (!phoneNum || !phoneCode || !userAccount || !userPassword) {
        layerWarn("请检验参数是否输入完整")
        return;
    }
    if (!userPasswordReg(userPassword)) {
        layerWarn("密码格式错误！8-16字符，仅支持英文,数字,下划线");
        return;
    }
    let param = {
        "userName": userAccount,
        "userPwd": userPassword,
        "phone": phoneNum,
        "code": phoneCode,
        "recommend": recommend
    }
    let data = await sendRequest("POST", "SysUser/registerUser", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            window.location.href = 'dashboard'
        }, 1000);
    }
}

/**
 * 注册用户
 */
async function registerUserByEmail(flag) {
    let recommend = getQueryString("code")
    let readme = $("#readme").is(":checked")
    if (!readme) {
        layerWarn("请勾选《服务用户协议》")
        return 0;
    }
    let emailString = $("#emailString").val()
    let emailCode = $("#emailCode").val()
    let userAccount = $("#userAccount").val()
    let userPassword = $("#userPassword1").val()
    console.log(emailCode, emailString, userAccount, userPassword)
    if (!emailString || !emailCode || !userAccount || !userPassword) {
        console.log(111)
        layerWarn("请检验参数是否输入完整")
        return;
    }
    if (!userPasswordReg(userPassword)) {
        layerWarn("密码格式错误！8-16字符，仅支持英文,数字,下划线");
        return;
    }
    let param = {
        "userName": userAccount,
        "userPwd": userPassword,
        "email": emailString,
        "code": emailCode,
        "recommend": recommend
    }
    let data = await sendRequest("POST", "SysUser/registerUserByEmail", JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            window.location.href = 'dashboard'
        }, 1000);
    }
}

// 支付按钮
function payMoney(id, buyTime) {
    if (!buyTime) {
        layerWarn("请输入购买时长")
        return;
    }
    let payType = $('input[name="payType"]:radio:checked').val();
    if (payType === '1') {
        // 余额充足
        turnModal('verifyModal', 'on')
    } else if (payType === '2' || payType === '3') {
        userRechargeCto(id, buyTime)
    }
}

/**
 * 直接创建一个订单
 */
async function createOrder(flowPackageId, buyTime) {
    if (!flowPackageId) {
        layerWarn("请选择流量包")
        return 0;
    }
    if (!buyTime) {
        layerWarn("请输入购买时长")
        return 0;
    }
    let data = await sendRequest("POST", "TransactionOrder/createBalanceTransactionOrder", {
        "flowPackageId": flowPackageId,
        "buyTime": buyTime
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            window.location.href = 'bought-flow-list'
        }, 1000);
    }
}


async function bandPurchasedFlow() {
    let id = $("#banedModalInputHidden").val()
    let banedReason = $("#banedReason").val()

    if (!id) {
        layerWarn("参数异常")
        return
    }
    let data = await sendRequest("POST", "RemovedFeature/disabled", {
        "purchasedFlowId": id,
        "remark": banedReason
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

async function unBandPurchasedFlow(id) {
    if (!id) {
        layerWarn("参数异常")
        return
    }
    let data = await sendRequest("POST", "RemovedFeature/disabled", {
        "purchasedFlowId": id,
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 将flow_deduction类型的订单支付
 * @param id
 */
function toPayFlowDeduction(id, balance, amount) {
    if (balance - amount >= 0) {
        $("#hiddenToId").val(id)
        turnModal('hasMoneyModal', 'on');
    } else {
        turnModal('noMoneyModal', 'on');
    }
}

async function useBalance2PayTransactionOrder() {
    let transactionId = $("#hiddenToId").val()
    if (!transactionId) {
        layerWarn("参数异常")
        return
    }
    let data = await sendRequest("POST", "TransactionOrder/useBalance2PayTransactionOrder", {
        "transactionId": transactionId,
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 提交人工实名认证信息
 */
async function submitAuthenticationRecord() {
    //TODO 获取参数
    let authenticationType = $("#authenticationType option:selected").val()
    let name = $("#name").val()
    let idCardNum = $("#idCardNum").val()
    const frontImg = $('#logoUploaderFront').get(0).files[0];
    if (!authenticationType) {
        layerWarn("请输入认证类型");
        return
    }
    const backImg = 'enterprise' === authenticationType ? $('#authFileUploader').get(0).files[0] : $('#logoUploaderBack').get(0).files[0];
    if (!name) {
        layerWarn("请输入证件名称");
        return
    }
    if (!idCardNum) {
        layerWarn("请输入证件号码");
        return
    }
    if (!frontImg) {
        layerWarn("请上传证件正面照！");
        return;
    }
    if (authenticationType === 'enterprise') {
        if (!backImg) {
            layerWarn("请上传企业授权书！");
            return;
        }
    } else {
        if (!idCardReg(idCardNum)) {
            layerWarn("证件号码格式错误！");
            return;
        }
        if (!backImg) {
            layerWarn("请上传证件背面照！");
            return;
        }
    }
    if (!verifySuffix(frontImg.name)) {
        layerWarn("证件正面文件格式不正确！");
        return;
    }
    let params = new FormData();
    params.append("authenticationType", authenticationType)
    params.append("name", name)
    params.append("idCardNum", idCardNum)
    params.append("frontImg", frontImg)
    params.append("backImg", backImg)
    let data = await sendFileUploadRequest("RealNameAuthentication/insertAuthenticationRecord", params);
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            window.location.href = 'authentication-real-name-list'
        }, 1000);
    }
}

function open2CheckAuthenticationModal(dataStr) {
    let data = JSON.parse(dataStr);
    $('#hiddenId').val(data.id);
    $('#realNameCheck').val(data.name);
    $('#idCardNumCheck').val(data.idCardNum);
    turnModal('checkAuthenticationModal', 'on');
}

async function authenticationAuditOperation() {
    let id = $("#hiddenId").val()
    let realNameCheck = $("#realNameCheck").val()
    console.log(realNameCheck)
    let idCardNumCheck = $("#idCardNumCheck").val()
    console.log(idCardNumCheck)
    if (!id) {
        layerWarn("唯一标识不存在")
        return;
    }
    if (!realNameCheck) {
        layerWarn("证件名称不存在")
        return;
    }
    if (!idCardNumCheck) {
        layerWarn("证件号码不存在")
        return;
    }
    let authenticationOperation = $('input[name="authenticationOperation"]:radio:checked').val();
    console.log(authenticationOperation)
    let authenticationRemark = $("#authenticationRemark").val()
    console.log(authenticationRemark)

    let params = {
        "id": id,
        "realName": realNameCheck,
        "idCardNum": idCardNumCheck,
        "authenticationOperation": authenticationOperation,
        "authenticationRemark": authenticationRemark
    }
    let data = await sendRequest("POST", "RealNameAuthentication/auditAuthenticationRecord", params);
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            window.location.href = 'authentication-real-name-list'
        }, 1000);
    }
}


async function submitWorkOrder() {
    // 获取工单类型
    let typeSelect = document.getElementById("typeId");
    let typeSelectOptionIndex = typeSelect.options.selectedIndex;
    let typeId = typeSelect.options[typeSelectOptionIndex].value;
    let typeValue = typeSelect.options[typeSelectOptionIndex].text;
    console.log(typeId, typeValue)
    if (!typeId) {
        return layerWarn("请选择工单类型")
    }
    // 获取紧急程度
    let urgentLevelSelect = document.getElementById("urgentLevel");
    let urgentLevelSelectOptionIndex = urgentLevelSelect.options.selectedIndex;
    let urgentLevelValue = urgentLevelSelect.options[urgentLevelSelectOptionIndex].value;
    console.log(urgentLevelValue)
    if (!urgentLevelValue) {
        return layerWarn("请选择工单紧急程度")
    }
    // 获取工单标题
    let orderTitle = $("#title").val()
    let domain = $("#domain").val()
    console.log(orderTitle)
    if (!orderTitle) {
        return layerWarn("请输入工单标题")
    }
    // 获取详情信息
    let details = $("#exampleFormControlTextarea1").val()
    console.log(details)
    if (!details) {
        return layerWarn("请输入工单详情")
    }
    let data = await sendRequest("POST", "WorkOrder/submitWorkOrder", {
        typeId: typeId,
        typeName: typeValue,
        urgentLevel: urgentLevelValue,
        title: orderTitle,
        remark: details,
        domain: domain,
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            window.location.href = 'order-list'
        }, 1000);
    }
}

/**
 * 批量删除工单分类
 */
async function deleteWorkOrderBatch() {
    let batchChecks = $('input[name="batchCheck"]:checkbox:checked');
    let ids = []
    $.each(batchChecks, function() {
        ids.push($(this).val())
    });
    if (!ids) {
        layerWarn("未选中任何数据");
        return;
    }
    let data = await sendRequest("POST", "WorkOrder/deleteBatch", {
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
async function deleteWorkOrder(id) {
    if (!id) {
        layerWarn("工单ID不能为空");
        return;
    }
    let data = await sendRequest("POST", "WorkOrder/delete", {
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
 * 结单
 */
async function stopWorkOrder(id) {
    if (!id) {
        layerWarn("工单ID不能为空");
        return;
    }
    // 获取紧急程度
    let resultSelect = document.getElementById("resultSelect");
    let resultSelectOptionIndex = resultSelect.options.selectedIndex;
    let result = resultSelect.options[resultSelectOptionIndex].value;
    console.log(result)
    if (!result) {
        return layerWarn("请选择工单处理结果")
    }
    let feedback = $("#feedback").val()
    let data = await sendRequest("POST", "WorkOrder/stop", {
        workOrderId: id,
        evaluationStars: result,
        feedback: feedback
    });
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 缓存预热提交接口
 */
async function submitCachePreheating() {
    let urlsParam = $("#cachePreheatingUrls").val()
    let urlArray = urlsParam.split(/\r?\n/);
    for (let i = 0; i < urlArray.length; i++) {
        if (!verifyUrlPrefix(urlArray[i])) {
            return layerWarn("请检查URL是否以http://或 https://开头")
        }
    }
    let params = {
        "urls": urlArray
    }
    console.log(params)
    let data = await sendRequest("POST", "CdnDomainCache/submitCachePreheating", params);
    if (data['code'] === 'SUCCESS') {
        layerSuccess("任务提交成功，请稍后在历史记录中查看状态。");
        setTimeout(function() {
            reload();
        }, 1500);
    } else {
        if (data['message'] && data['message'].includes('http')) {
            layerFail("操作失败：每日预热URL额度可能已用尽。");
        } else {
            autoLayer(data);
        }
    }
}


/**
 * 文件刷新，目录刷新提交接口
 */
async function submitCacheRefresh(typeFunction) {
    let urlsParam = $("#exampleFormControlTextarea1").val()
    let urlArray = urlsParam.split(/\r?\n/);
    if (typeFunction === "file") {
        for (let i = 0; i < urlArray.length; i++) {
            if (!verifyUrlPrefix(urlArray[i])) {
                return layerWarn("请检查URL是否以http://或 https://开头")
            }
        }
    } else if (typeFunction === "directory") {
        for (let i = 0; i < urlArray.length; i++) {
            if (!verifyUrlPrefixWithEnd(urlArray[i])) {
                return layerWarn("请检查URL是否以http://或 https://开头，并且以/结尾")
            }
        }
    } else {
        return layerWarn("参数异常");
    }
    let params = {
        "urls": urlArray,
        "type": typeFunction
    }
    console.log(params)
    let data = await sendRequest("POST", "CdnDomainCache/submitCacheRefresh", params);
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

/**
 * 将当前用户全部消息改为已读状态
 * @returns {Promise<void>}
 */
async function readAllMessage() {
    let params = {}
    let data = await sendRequest("POST", "Message/readAllMessage", params);
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

async function readMessage(id) {
    let params = {
        id: id
    }
    let data = await sendRequest("POST", "Message/readMessage", params);
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

async function giftExchange() {
    let giftCode = $('#giftCode').val()
    if (!giftCode) {
        layerWarn("请输入兑换码～")
        return
    }
    let params = {
        giftCode: giftCode
    }
    let data = await sendRequest("POST", "RemovedFeature/disabled", params);
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload()
        }, 1000);
    }
}

async function saveIgnoreQueryString(id) {
    let isChecked = $("#ignore_query_string_status").is(":checked");
    let enable = isChecked ? "on" : "off";
    let type = $("input[name='iqs_type']:checked").val();
    let hashKeyArgs = $("#hashKeyArgs").val();

    if (isChecked && (!type || !hashKeyArgs)) {
        layerWarn("启用过滤参数时，必须选择过滤类型并填写参数列表。");
        return;
    }

    // 验证参数列表格式
    if (isChecked && hashKeyArgs) {
        // 检查是否使用英文逗号分隔
        if (hashKeyArgs.includes(';') || hashKeyArgs.includes('；')) {
            layerWarn("参数列表必须使用英文逗号(,)分隔，请勿使用分号。");
            return;
        }

        // 检查是否包含非法字符
        if (!/^[a-zA-Z0-9_,]+$/.test(hashKeyArgs)) {
            layerWarn("参数列表只能包含字母、数字、下划线和英文逗号。");
            return;
        }
    }

    let param = {
        "enable": enable,
        "type": type,
        "hashKeyArgs": hashKeyArgs
    };

    let data = await sendRequest("POST", "CdnDomainCache/saveIgnoreQueryString?doMainId=" + id, JSON.stringify(param), "application/json");
    autoLayer(data);
    if (data['code'] === 'SUCCESS') {
        setTimeout(function() {
            reload();
        }, 1000);
    }
}

function freePackage() {
    const n = Date.now(),
        w = navigator.language
    loadingShow()
    $.ajax({
        type: 'POST',
        url: '/RemovedFeature/disabled',
        data: { t: n, a: w },
        complete: function() {
            loadingRemove()
        },
        success: function(data) {
            layerInfo(data.message)
        },
        error: function(xhr, state, errorThrown) {
            if ('responseJSON' in xhr && 'message' in xhr.responseJSON) {
                layerFail(xhr.responseJSON.message);
            } else {
                layerFail('请求失败，请检查网络情况或稍后再试！');
            }
        }
    })
}

/**
 * 切换URL鉴权选项的显示/隐藏
 */
function toggleUrlAuthOptions() {
    const isChecked = $("#urlAuthStatusSwitch").is(":checked");
    if (isChecked) {
        $("#urlAuthOptions").slideDown(200);
    } else {
        $("#urlAuthOptions").slideUp(200);
    }
}

/**
 * 生成随机密钥
 * @param {string} inputId - 要填充的输入框ID
 */
function generateRandomKey(inputId) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    const length = 16; // 生成16位密钥
    let result = '';

    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }

    $(`#${inputId}`).val(result);

    // 显示提示
    const $input = $(`#${inputId}`);
    const $parent = $input.closest('.col-sm-9');

    // 移除可能存在的成功提示
    $parent.find('.generate-success-message').remove();

    // 添加新的成功提示
    const $successMessage = $('<div class="generate-success-message text-success small mt-1"><i class="bi bi-check-circle"></i> 已生成随机密钥</div>');
    $parent.append($successMessage);

    // 2秒后淡出提示
    setTimeout(() => {
        $successMessage.fadeOut(500, function() {
            $(this).remove();
        });
    }, 2000);
}

async function saveUrlAuth(id) {
    const status = $("#urlAuthStatusSwitch").is(":checked") ? "on" : "off";
    const type = $("#authTypeSelect").val();
    const primaryKey = $("#primaryKeyInput").val().trim();
    const secondaryKey = $("#secondaryKeyInput").val().trim();
    const expireTime = $("#expireTimeInput").val();

    // 如果关闭状态，直接保存
    if (status === "off") {
        let param = {
            "doMainId": id,
            "urlAuth": {
                "status": status
            }
        };

        let data = await sendRequest("POST", "CdnDomainAccess/saveUrlAuth", JSON.stringify(param), "application/json");
        autoLayer(data);
        if (data['code'] === 'SUCCESS') {
            setTimeout(function() {
                reload();
            }, 1000);
        }
        return;
    }

    // 启用状态下的验证
    if (!type) {
        layerWarn("请选择鉴权类型");
        return;
    }

    if (!primaryKey) {
        layerWarn("主密钥不能为空");
        $("#primaryKeyInput").focus();
        return;
    }

    const keyRegex = /^[a-zA-Z0-9]{6,128}$/;
    if (!keyRegex.test(primaryKey)) {
        layerWarn("主密钥必须由6-128位的大小写字母或数字组成");
        $("#primaryKeyInput").focus();
        return;
    }

    if (secondaryKey && !keyRegex.test(secondaryKey)) {
        layerWarn("备密钥必须由6-128位的大小写字母或数字组成");
        $("#secondaryKeyInput").focus();
        return;
    }

    if (!expireTime) {
        layerWarn("过期时间不能为空");
        $("#expireTimeInput").focus();
        return;
    }

    const expireTimeNum = parseInt(expireTime);
    if (isNaN(expireTimeNum) || expireTimeNum < 0) {
        layerWarn("过期时间必须是大于等于0的整数");
        $("#expireTimeInput").focus();
        return;
    }

    // 显示保存中状态
    const $saveBtn = $("button[onclick*='saveUrlAuth']");
    const originalHtml = $saveBtn.html();
    $saveBtn.html('<i class="bi bi-hourglass-split me-1"></i> 保存中...').prop('disabled', true);

    let param = {
        "doMainId": id,
        "urlAuth": {
            "status": status,
            "type": type,
            "primary_key": primaryKey,
            "secondary_key": secondaryKey || "",
            "expire_time": expireTimeNum
        }
    };

    try {
        let data = await sendRequest("POST", "CdnDomainAccess/saveUrlAuth", JSON.stringify(param), "application/json");
        autoLayer(data);
        if (data['code'] === 'SUCCESS') {
            setTimeout(function() {
                reload();
            }, 1000);
        }
    } catch (error) {
        layerFail("保存失败，请稍后重试");
    } finally {
        // 恢复按钮状态
        $saveBtn.html(originalHtml).prop('disabled', false);
    }
}
