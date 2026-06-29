/**
 * 邮箱格式正则验证
 * @param email
 * @returns {boolean}
 */
function emailReg(email) {
    let reg = /^[\da-zA-Z]+([\-\.\_]?[\da-zA-Z]+)*@[\da-zA-Z]+([\-\.]?[\da-zA-Z]+)*(\.[a-zA-Z]{2,})+$/i;
    return reg.test(email);
}

/**
 * 手机号码格式正则验证
 * @param phone
 * @returns {boolean}
 */
function phoneReg(phone) {
    let reg = /^1[3456789]\d{9}$/;
    return reg.test(phone);
}

/**
 * 用户名正则：最长10字符，支持中文、英文
 * @param name
 * @returns {boolean}
 */
function userNameReg(name) {
    let reg = /^([\u4E00-\uFA29]|[\uE7C7-\uE7F3]|[a-zA-Z]){1,10}$/;
    return reg.test(name);
}

/**
 * 密码正则：8-16字符，支持字母、数字以及特殊符号
 * @param password
 * @returns {boolean}
 */
function userPasswordReg(password) {
    // let reg = /^(\w){8,16}$/;
    // let reg =  /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+`\-={}:";'<>?,.\/]).{8,16}$/;
    let reg = /^[A-Za-z0-9~!@#$%^&*()_+`\-={}:";'<>?,.\/]{8,16}$/;
    return reg.test(password);
}

/**
 * 域名正则
 * @param domain
 * @returns {boolean}
 */
function domainReg(domain) {
    // let reg = /^(?=^.{3,255}$)(http(s)?:\/\/)?(www\.)?[a-zA-Z0-9][-a-zA-Z0-9]{0,62}(\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62})$/;
    // let reg = /^(?:[-A-Za-z0-9]+\.)+[A-Za-z]{2,}$/;
    // const reg = /^(?:[-A-Za-z0-9\u4E00-\u9FFF]+\.)+[A-Za-z\u4E00-\u9FFF]{2,}$/;
    // return reg.test(domain);
    return true;
}

function hasScheme(domain) {
    const reg = /^https?:\/\/.+/;
    return reg.test(domain);
}

/**
 * IP地址正则
 * @param ip
 * @returns {boolean}
 */
function ipReg(ip) {
    let reg = /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/;
    return reg.test(ip);
}

/**
 * 金额正则
 * @param number
 * @returns {boolean}
 */
function numberReg(number) {
    let reg = /^(\d|([1-9]\d+))(\.\d{1,2})?$/;
    return reg.test(number);
}

/**
 * 身份证正则 支持1代2代
 * @param idCard
 * @returns {boolean}
 */
function idCardReg(idCard) {
    let reg = /^\d{6}((((((19|20)\d{2})(0[13-9]|1[012])(0[1-9]|[12]\d|30))|(((19|20)\d{2})(0[13578]|1[02])31)|((19|20)\d{2})02(0[1-9]|1\d|2[0-8])|((((19|20)([13579][26]|[2468][048]|0[48]))|(2000))0229))\d{3})|((((\d{2})(0[13-9]|1[012])(0[1-9]|[12]\d|30))|((\d{2})(0[13578]|1[02])31)|((\d{2})02(0[1-9]|1\d|2[0-8]))|(([13579][26]|[2468][048]|0[048])0229))\d{2}))(\d|X|x)$/;
    return reg.test(idCard);
}

/** 端口正则验证*/
function portReg(port) {
    if (/^[1-9]\d*|0$/.test(port) && port * 1 >= 0 && port * 1 <= 65535) {
        return true
    }
    return false;
}

/** url正则验证*/
function urlReg(url) {
    let reg = /http(s)?:\/\/([\w-]+\.)+[\w-]+(\/[\w- .\/?%&=]*)?/;
    return !!reg.test(url);
}

/** QQ号正则验证*/
function qqReg(qq) {
    let reg = /^[1-9][0-9]{4,9}$/gim;
    return reg.test(qq);
}

/** 正整数正则*/
function integerReg(positiveInteger) {
    let reg = /^\d+$/;
    return reg.test(positiveInteger);
}

/** 图片格式正则*/
function verifySuffix(fileName) {
    let suffix = `(bmp|jpg|png|tif|gif|pcx|tga|exif|fpx|svg|psd|cdr|pcd|dxf|ufo|eps|ai|raw|WMF|webp|jpeg)`
    let regular = new RegExp(`.*\.${suffix}`)
    return regular.test(fileName)
}

/** 含有http或者https的正则*/
function verifyUrlPrefix(url) {
    let reg = /^(http|https):\/\/[^\s]+$/i;
    return reg.test(url)
}

/** 含有http或者https的正则*/
function verifyUrlPrefixWithEnd(url) {
    let reg = /^(https?:\/\/).*\/$/;
    return reg.test(url)
}

/** 正则验证是否是不小于1的小数，小数位数不超过2*/
function validatePositiveDecimal(input) {
    let regex = /^(?!0$|0\.0{0,2}$|1$|1\.0{0,2}$)[0-9](\.[0-9]{1,2})?$/;
    return regex.test(input);
}

/**域名配置的一些正则*/
function certNameReg(certName) {
    let regex = /^[a-zA-Z0-9_-]{3,64}$/;
    return regex.test(certName);
}

function dirpathReg(dirpath) {
    const regex = /^(\/[^\/]+)+$|^(\/[^\/]+)+\/$/;
    return regex.test(dirpath);
}

function fileSuffixReg(fileSubffix) {
    let regex = /^\.?[a-zA-Z0-9_]+([,;，；]\.?[a-zA-Z0-9_]+)*$/;
    return regex.test(fileSubffix);
}

function fullDirPathRef(fullDirPath) {
    let regex = /^(?:\/[^\/]+)*\/\w+(?:\.\w+)?$/;
    return regex.test(fullDirPath);
}

//匹配186.33.22.1 blog.kedaya.site *.kedaya.site这三种
function antiLeechLinkReg(antiLeechLink) {
    let regex = /^(?:\*\.)?(?:[a-zA-Z0-9_-]+\.){1,}[a-zA-Z]{2,}$|\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;
    return regex.test(antiLeechLink);
}

//以下是一个可以匹配IPv4和IPv6地址以及CIDR块的JavaScript正则表达式：
function ipBlackWhiteReg(ipBlackWhite) {
    let regex = /\b(?:\d{1,3}\.){3}\d{1,3}(?:\/\d{1,2})?|[a-fA-F\d:]+:+[a-fA-F\d:]+\b/;
    return regex.test(ipBlackWhite);
}


function uaHuaWeiReg(ua) {
    let regex = /^[0-9a-zA-Z*.\-_();,\/\s]+$/g;
    return regex.test(ua);
}

function uaVolReg(ua) {
    let regex = /^(?![\u4e00-\u9fa5])(?! *$)(?!\*$)(?:\*?(?:[A-Za-z0-9])+\*?)$/;
    return regex.test(ua);
}
