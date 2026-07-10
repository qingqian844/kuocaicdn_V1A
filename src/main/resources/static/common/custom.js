/**
 * 获取Cookie
 * @param key 键
 * @returns {string|null}
 */
function getCookie(key) {
    return (
        decodeURIComponent(
            document.cookie.replace(
                new RegExp(
                    "(?:(?:^|.*;)\\s*" +
                    encodeURIComponent(key).replace(/[-.+*]/g, "\\$&") +
                    "\\s*\\=\\s*([^;]*).*$)|^.*$",
                ),
                "$1",
            ),
        ) || null
    );
}

/**
 * 创建Cookie
 * @param key 键
 * @param value 值
 * @param days 过期天数
 */
function setCookie(key, value, days) {
    const d = new Date();
    d.setTime(d.getTime() + (days * 24 * 60 * 60 * 1000));
    const expires = "expires=" + d.toGMTString();
    document.cookie = key + "=" + value + "; " + expires;
}

function isSafeRelativeUrl(url) {
    if (!url) {
        return false;
    }
    const value = String(url).trim();
    if (!value || value.startsWith('//') || value.includes('\\') || value.includes('\r') || value.includes('\n')) {
        return false;
    }
    try {
        const parsed = new URL(value, window.location.origin);
        return parsed.origin === window.location.origin;
    } catch (e) {
        return false;
    }
}

function safeRedirect(url, fallback) {
    window.location.href = isSafeRelativeUrl(url) ? url : (fallback || 'dashboard');
}

/**
 * 将指定元素的value复制到剪切板
 * @param id
 */
function copy(id) {
    const input = document.querySelector('#' + id);
    input.select();
    if (document.execCommand('copy')) {
        document.execCommand('copy');
        layerSuccess('复制成功')
    }
}

/**
 * 获取当前url的参数
 * @param name
 * @returns {null|string}
 */
function getQueryString(name) {
    let reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    let r = window.location.search.substr(1).match(reg);
    if (r != null) {
        return decodeURIComponent(r[2]);
    }
    ;
    return null;
}

/**
 * 暂未开发
 */
function unDevelop() {
    layer.msg("该功能暂未开发！");
}

/**
 * 60秒倒计时
 * @param o
 */
function time(o, wait) {
    if (wait === 0) {
        $(o).attr("disabled", false);
        $(o).html("重新获取");
    } else {
        $(o).attr("disabled", true);
        $(o).html(wait + "秒");
        wait--;
        setTimeout(function () {
            time(o, wait);
        }, 1000);
    }
}

/**
 * 元素抖动
 * @param o
 */
function errorShake(o) {
    shake(o);
    var $panel = $("#" + o);
    $panel.css("border", "1px solid red");
}

/**
 * 元素抖动
 * @param o
 */
function shake(o) {
    var $panel = $("#" + o);
    box_left = 0;
    //box_left = $panel.css('left');
    //box_left = ($(window).width() -  $panel.width()) / 2;
    $panel.css({'left': box_left, 'position': 'relative'});
    for (var i = 1; 4 >= i; i++) {
        $panel.animate({left: box_left - (20 - 5 * i)}, 30);
        $panel.animate({left: box_left + (20 - 5 * i)}, 30);
    }
}

function playSuccess() {
    let mp3 = new Audio('common/music/tip.mp3') // 创建音频对象
    mp3.play() // 播放
}

function playWarn() {
    let mp3 = new Audio('common/music/warn.mp3') // 创建音频对象
    mp3.play() // 播放
}

function playFail() {
    let mp3 = new Audio('common/music/error.mp3') // 创建音频对象
    mp3.play() // 播放
}

/**
 * 警告
 * @param msg
 */
function layerWarn(msg) {
    if (!msg) {
        msg = '警告';
    }
    console.log(`警告：${msg}`)
    // playWarn();
    let toast = `<div id="liveToastW" class="position-fixed toast hide" role="alert" aria-live="assertive" aria-atomic="true" style="top: 20px; right: 20px; z-index: 1000;">
                  <div class="toast-header">
                    <div class="d-flex align-items-center flex-grow-1">
                      <div class="flex-shrink-0">
                        <i class="bi bi-circle-fill text-warning"></i>
                      </div>
                      <div class="flex-grow-1 ms-3">
                        <h5 class="mb-0">警告</h5>
                      </div>
                     
                    </div>
                  </div>
                  <div class="toast-body">
                    ` + msg + `
                  </div>
                </div>`
    $('body').append($(toast));
    $('#liveToastW').slideDown(200);
    setTimeout(function () {
        $('#liveToastW').slideUp(200);
        $('#liveToastW').remove();
    }, 2000);
}


/**
 * 成功
 * @param msg
 */
function layerSuccess(msg) {
    if (!msg) {
        msg = '操作成功';
    }
    console.log(`操作成功：${msg}`)
    // playSuccess();
    let toast = `<div id="liveToastS" class="position-fixed toast hide" role="alert" aria-live="assertive" aria-atomic="true" style="top: 20px; right: 20px; z-index: 1000;">
                  <div class="toast-header">
                    <div class="d-flex align-items-center flex-grow-1">
                      <div class="flex-shrink-0">
                        <i class="bi bi-circle-fill text-success"></i>
                      </div>
                      <div class="flex-grow-1 ms-3">
                        <h5 class="mb-0">成功</h5>
                      </div>
                     
                    </div>
                  </div>
                  <div class="toast-body">
                    ` + msg + `
                  </div>
                </div>`
    $('body').append($(toast));
    $('#liveToastS').slideDown(200);
    setTimeout(function () {
        $('#liveToastS').slideUp(200);
        $('#liveToastS').remove();
    }, 2000);
}


/**
 * 失败
 * @param msg
 */
function layerFail(msg) {
    if (!msg) {
        msg = '操作失败';
    }
    console.log(`操作失败：${msg}`)
    // playFail();
    let toast = `<div id="liveToastF" class="position-fixed toast hide" role="alert" aria-live="assertive" aria-atomic="true" style="top: 20px; right: 20px; z-index: 1000;">
                  <div class="toast-header">
                    <div class="d-flex align-items-center flex-grow-1">
                      <div class="flex-shrink-0">
                        <i class="bi bi-circle-fill text-danger"></i>
                      </div>
                      <div class="flex-grow-1 ms-3">
                        <h5 class="mb-0">失败</h5>
                      </div>
                     
                    </div>
                  </div>
                  <div class="toast-body">
                    ` + msg + `
                  </div>
                </div>`
    $('body').append($(toast));
    $('#liveToastF').slideDown(200);
    setTimeout(function () {
        $('#liveToastF').slideUp(200);
        $('#liveToastF').remove();
    }, 3000);
}

/**
 * 通知
 * @param msg
 */
function layerInfo(msg) {
    if (!msg) {
        msg = '通知';
    }
    console.log(`通知：${msg}`)
    // playSuccess();
    let toast = `<div id="liveToastI" class="position-fixed toast hide" role="alert" aria-live="assertive" aria-atomic="true" style="top: 20px; right: 20px; z-index: 1000;">
                  <div class="toast-header">
                    <div class="d-flex align-items-center flex-grow-1">
                      <div class="flex-shrink-0">
                        <i class="bi bi-circle-fill text-primary"></i>
                      </div>
                      <div class="flex-grow-1 ms-3">
                        <h5 class="mb-0">通知</h5>
                      </div>
                     
                    </div>
                  </div>
                  <div class="toast-body">
                    ` + msg + `
                  </div>
                </div>`
    $('body').append($(toast));
    $('#liveToastI').slideDown(200);
    setTimeout(function () {
        $('#liveToastI').slideUp(200);
        $('#liveToastI').remove();
    }, 2000);
}

/**
 * 弹除提示
 * @param data
 */
function autoLayer(data) {
    if (!data) {
        layerFail();
    } else {
        if (data['code'] === 'SUCCESS') {
            layerSuccess(data['message']);
        } else {
            layerFail(data['message']);
        }
    }
}

/**
 * 刷新页面
 */
function reload() {
    window.location.reload();
}

/**
 * 显示模态框
 * @param id
 * @param operation
 */
function turnModal(id, operation) {
    let myModal = new bootstrap.Modal(document.getElementById(id), {})
    if (operation === "on") {
        myModal.show();
    }
    if (operation === "off") {
        myModal.hide();
    }
}

function loading() {
    loadingShow();
    loadingRemove();
}

function loadingShow() {
    $('#loadingDiv').remove();
}

function loadingRemove() {
    // setTimeout(function () {
    //     $('#loadingDiv').remove();
    // }, 1000);
    $('#loadingDiv').remove();
}
$(function () {
    $('#loadingDiv, #dashboardStatisticsLoadingMask').remove();
    if ($('#disableLegacyLoadingStyle').length === 0) {
        $('head').append('<style id="disableLegacyLoadingStyle">#loadingDiv,#dashboardStatisticsLoadingMask,.dashboard-statistics-loading-mask{display:none!important;}</style>');
    }
});

const api_origin = 'https://api.kuocaicdn.com';

/**
 * 发送请求
 * @param method 请求方法
 * @param url 请求地址
 * @param data 请求参数数据
 * @param contentType 请求类型
 * @param loading 加载动画
 * @returns {Promise<unknown>}
 */
function sendRequest(method, url, data, contentType = 'application/x-www-form-urlencoded; charset=UTF-8', loading = true) {
    return new Promise(function (resolve, reject) {
        $.ajax({
            type: method, url: url,
            data: data,
            contentType: contentType,
            dataType: 'json',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            },
            beforeSend: function () {
                if (loading) {
                    loadingShow();
                }
            },
            complete: function () {
                if (loading) {
                    loadingRemove();
                }
            },
            success: function (data) {
                resolve(data);
            },
            error: function (xhr, state, errorThrown) {
                reject(errorThrown);
                let message = getRequestErrorMessage(xhr, errorThrown);
                if (message) {
                    layerFail(message);
                } else {
                    layerFail('请求失败，请检查网络情况或稍后再试！');
                }
            }
        });
    });
}

function getRequestErrorMessage(xhr, errorThrown) {
    if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
        return xhr.responseJSON.message;
    }
    if (xhr && xhr.responseText) {
        if (/^\s*<!doctype html/i.test(xhr.responseText) || /^\s*<html/i.test(xhr.responseText)) {
            if (xhr.status === 401 || xhr.status === 302 || xhr.status === 405) {
                return '登录已过期，请刷新页面后重新登录';
            }
            return '页面状态已变化，请刷新页面后重新提交';
        }
        try {
            let response = JSON.parse(xhr.responseText);
            if (response && response.message) {
                return response.message;
            }
        } catch (e) {
        }
    }
    if (xhr && xhr.status === 401) {
        return '登录已过期，请重新登录';
    }
    if (xhr && xhr.status === 405) {
        return '页面脚本已过期，请刷新页面后重新提交';
    }
    return errorThrown;
}

/**
 * 发送上传文件请求
 * @param url 请求地址
 * @param formData 请求参数数据
 * @returns {Promise<unknown>}
 */
function sendFileUploadRequest(url, formData, loading = true) {
    return new Promise(resolve => {
        $.ajax({
            type: "POST",
            url: url,
            data: formData,
            dataType: "json",
            contentType: false,
            processData: false,
            beforeSend: function () {
                if (loading) {
                    loadingShow();
                }
            },
            complete: function () {
                if (loading) {
                    loadingRemove();
                }
            },
            success: function (data) {
                resolve(data);
            },
            error: function (xhr, state, errorThrown) {
                layerFail("上传失败，请检查网络情况或稍后再试！")
            }
        });
    });
}

function hello(msg = '成功') {
    layerSuccess(msg)
}

// // 监听开发者工具栏在页面的框口大小变化，并跳转至百度
// var h = window.innerHeight, w = window.innerWidth;
// window.onresize = function () {
//     if (h != window.innerHeight || w != window.innerWidth) {
//         window.close();
//         window.location.replace("https://www.baidu.com/")
//     }
// }

/**
 * 确认删除操作
 * @param callback
 */
function confirmDelete(callback) {
    let confirm = `
        <div id="confirmModal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title" id="exampleModalCenterTitle"><i class="bi bi-exclamation-circle-fill text-danger"></i> 删除操作</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body">
                <p>删除操作不可逆，确认删除吗？</p>
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

function confirmUnBanned(userId, userName) {
    let confirm = `
        <div id="unBannedModal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title" id="exampleModalCenterTitle"><i class="bi bi-exclamation-circle-fill text-danger"></i> 解封用户</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body">
                <p>确认解封 ${userName} 用户吗？</p>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-white" data-bs-dismiss="modal">取消</button>
                <button type="button" class="btn btn-primary" onclick="unBannedUser('${userId}')">确认</button>
              </div>
            </div>
          </div>
        </div>
    `
    $('body').append($(confirm));
    turnModal('unBannedModal', 'on')
}

function confirmBanned(userId, userName) {
    const tpl = `
    <div class="modal fade" id="bannedUserModal" tabindex="-1" aria-labelledby="bannedUserLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h1 class="modal-title fs-5" id="bannedUserLabel">封禁用户</h1>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form>
                        <div class="mb-3">
                            <label for="banned-user-id" class="col-form-label">用户 ID：</label>
                            <input type="text" class="form-control" id="banned-user-id" readonly>
                        </div>
                        <div class="mb-3">
                            <label for="banned-reason" class="col-form-label">封禁原因：</label>
                            <textarea class="form-control" id="banned-reason"></textarea>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="button" class="btn btn-primary" id="btn-banned-user">确认</button>
                </div>
            </div>
        </div>
    </div>`
    let $model = document.getElementById('bannedUserModal');
    if (null === $model) {
        const div = document.createElement('div');
        div.innerHTML = tpl;
        document.body.appendChild(div.children[0]);
        $model = document.getElementById('bannedUserModal');
    }
    const $label = document.getElementById('bannedUserLabel'), $reason = document.getElementById('banned-reason'), $userId = document.getElementById('banned-user-id'), $btn = document.getElementById('btn-banned-user');
    function confirm() {
        bannedUser(userId, $reason.value);
    }
    const myModal = new bootstrap.Modal($model, {});
    $model.addEventListener('show.bs.modal', function (event) {
        $label.innerText = `封禁用户：${userName}`;
        $userId.value = userId;
    }, false);
    $model.addEventListener('hidden.bs.modal', function (event) {
        $reason.value = '';
        $btn.removeEventListener('click', confirm, false);
    }, false);
    $btn.addEventListener('click', confirm, false);
    myModal.show();
}

/**
 * 获取 TomSelect 多选值
 * @param id
 * @returns {*[]}
 */
function getTomSelectValues(id) {
    let ids = []
    let idSelect = $('#' + id).find("option:selected");
    for (let i = 0; i < idSelect.length; i++) {
        ids.push(idSelect[i].value);
    }
    return ids;
}
