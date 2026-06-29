package com.kuocai.cdn.api.tencent.cdn;

import com.tencentcloudapi.common.exception.TencentCloudSDKException;

import java.util.HashMap;
import java.util.Map;

public class TencentErrorCodeHandler {
    private static final Map<String, String> errorCodeMap;

    static {
        errorCodeMap = new HashMap<>(250);
        errorCodeMap.put("AuthFailure", "CAM签名/鉴权错误。");
        errorCodeMap.put("FailedOperation.CdnConfigError", "域名配置更新操作失败，请重试或联系客服人员解决。");
        errorCodeMap.put("InternalError.CamSystemError", "内部鉴权系统错误。");
        errorCodeMap.put("InternalError.CdnConfigError", "域名配置更新失败。");
        errorCodeMap.put("InternalError.CdnDbError", "内部数据错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InternalError.CdnQueryParamError", "内部查询错误，请重试或联系客服人员解决。");
        errorCodeMap.put("InternalError.CdnQuerySystemError", "内部查询错误，请重试或联系客服人员解决。");
        errorCodeMap.put("InternalError.CdnSystemError", "系统错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InternalError.ClsInternalError", "日志服务内部错误。");
        errorCodeMap.put("InternalError.CostDataSystemError", "计费数据内部查询错误，请重试或联系客服人员解决。");
        errorCodeMap.put("InternalError.DataSystemError", "数据查询错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InternalError.Error", "内部服务错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InternalError.InvalidErrorCode", "内部服务错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InternalError.ProxyServer", "内部服务错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InternalError.RouteError", "内部服务错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InternalError.ScdnUserNoPackage", "SCDN服务未生效，请购买或续费SCDN套餐后重试。");
        errorCodeMap.put("InternalError.ScdnUserSuspend", "安全加速服务已停服，请重新购买套餐后开启。");
        errorCodeMap.put("InternalError.SystemDBError", "内部数据错误，请重试或联系客服人员解决。");
        errorCodeMap.put("InternalError.SystemError", "内部服务错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InternalError.TagSystemError", "标签内部错误，请重试或联系客服人员解决。");
        errorCodeMap.put("InternalError.UnknownError", "内部服务错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InvalidParameter.AccessPortOpenedHttps", "域名启用 HTTPS 配置需保持访问端口配置-443端口为开启状态。");
        errorCodeMap.put("InvalidParameter.BandLimitRequiredMainland", "请删除域名的限流管理配置后再切换加速区域。");
        errorCodeMap.put("InvalidParameter.BandwidthAlertCounterMeasureConflictOriginType", "源站类型为COS源或第三方对象存储的域名，用量封顶-超出阈值的处理方式仅支持访问返回404，请修改该配置后重试。");
        errorCodeMap.put("InvalidParameter.CDNStatusInvalidDomain", "域名状态不合法。");
        errorCodeMap.put("InvalidParameter.CamResourceBelongToDifferentUser", "同一次请求的资源AppId不一致。");
        errorCodeMap.put("InvalidParameter.CamResourceSixStageError", "资源六段式标记参数错误。");
        errorCodeMap.put("InvalidParameter.CamTagKeyAlreadyAttached", "域名已与该标签关联，请勿重复操作。");
        errorCodeMap.put("InvalidParameter.CamTagKeyIllegal", "标签键字符不合法。");
        errorCodeMap.put("InvalidParameter.CamTagKeyNotExist", "标签键不存在。");
        errorCodeMap.put("InvalidParameter.CamTagValueIllegal", "标签值字符不合法。");
        errorCodeMap.put("InvalidParameter.CdnCertInfoNotFound", "证书信息不存在或非法，请确认后重试。");
        errorCodeMap.put("InvalidParameter.CdnCertNoCertInfo", "证书无效，请确认后重试。");
        errorCodeMap.put("InvalidParameter.CdnCertNotPem", "HTTPS证书无效。");
        errorCodeMap.put("InvalidParameter.CdnClsDuplicateTopic", "存在重复主题。");
        errorCodeMap.put("InvalidParameter.CdnClsTopicNameInvalid", "主题名字不合法。");
        errorCodeMap.put("InvalidParameter.CdnClsTopicNotExist", "CLS主题不存在。");
        errorCodeMap.put("InvalidParameter.CdnConfigInvalidCache", "缓存配置不合法，请确认后重试。");
        errorCodeMap.put("InvalidParameter.CdnConfigInvalidHost", "域名不符合规范，请确认域名是否符合规范。");
        errorCodeMap.put("InvalidParameter.CdnConfigInvalidTag", "标签配置不合法。");
        errorCodeMap.put("InvalidParameter.CdnConfigTagRequired", "域名添加失败，当前域名必须选择标签，请确认后重试。");
        errorCodeMap.put("InvalidParameter.CdnHostHasSpecialConfig", "域名拥有特殊配置，需人工处理。");
        errorCodeMap.put("InvalidParameter.CdnHostInternalHost", "该域名属于指定账号域名，不允许接入。");
        errorCodeMap.put("InvalidParameter.CdnHostInvalidMiddleConfig", "错误的中间源配置。");
        errorCodeMap.put("InvalidParameter.CdnHostInvalidParam", "域名格式不合法，请确认后重试。");
        errorCodeMap.put("InvalidParameter.CdnHostInvalidStatus", "域名状态不合法。");
        errorCodeMap.put("InvalidParameter.CdnHostIsCosDefaultAccess", "该域名为COS访问域名，无法接入，如需启动加速服务，请前往COS控制台启用默认 CDN 加速域。");
        errorCodeMap.put("InvalidParameter.CdnHostTooLongHost", "域名太长。");
        errorCodeMap.put("InvalidParameter.CdnInterfaceError", "内部接口错误，请联系客服人员进一步排查。");
        errorCodeMap.put("InvalidParameter.CdnInvalidParamInterval", "参数Interval错误，请确认后重试。");
        errorCodeMap.put("InvalidParameter.CdnInvalidParamMetric", "参数Metric错误，请检查后重试。");
        errorCodeMap.put("InvalidParameter.CdnInvalidParamServiceType", "ServiceType字段不合法，请确认后重试。");
        errorCodeMap.put("InvalidParameter.CdnKeyRulesExcludeCustomRequiresFullLego", "配置暂不支持开启该配置。");
        errorCodeMap.put("InvalidParameter.CdnKeyRulesInvalidQueryStringValue", "QueryString字段不合法，请确认后重试。");
        errorCodeMap.put("InvalidParameter.CdnParamError", "参数错误，请参考文档中示例参数填充。");
        errorCodeMap.put("InvalidParameter.CdnPurgeWildcardNotAllowed", "刷新不支持泛域名。");
        errorCodeMap.put("InvalidParameter.CdnPushWildcardNotAllowed", "预热不支持泛域名。");
        errorCodeMap.put("InvalidParameter.CdnStatInvalidDate", "日期不合法，请参考文档中日期示例。");
        errorCodeMap.put("InvalidParameter.CdnStatInvalidFilter", "统计维度不合法，请参考文档中统计分析示例。");
        errorCodeMap.put("InvalidParameter.CdnStatInvalidMetric", "统计类型不合法，请参考文档中统计分析示例。");
        errorCodeMap.put("InvalidParameter.CdnStatInvalidProjectId", "项目ID错误，请确认后重试。");
        errorCodeMap.put("InvalidParameter.CdnStatTooManyDomains", "查询的域名数量超过限制。");
        errorCodeMap.put("InvalidParameter.CdnUrlExceedLength", "URL 超过限制长度。");
        errorCodeMap.put("InvalidParameter.ClsIndexConflict", "索引冲突。");
        errorCodeMap.put("InvalidParameter.ClsIndexRuleEmpty", "索引规则为空。");
        errorCodeMap.put("InvalidParameter.ClsInvalidContent", "无效内容。");
        errorCodeMap.put("InvalidParameter.ClsInvalidContentType", "无效的Content-Type。");
        errorCodeMap.put("InvalidParameter.ClsInvalidParam", "参数错误，请检查后重试。");
        errorCodeMap.put("InvalidParameter.ClsLogsetConflict", "日志集冲突。");
        errorCodeMap.put("InvalidParameter.ClsLogsetEmpty", "日志集为空。");
        errorCodeMap.put("InvalidParameter.ClsLogsetNotEmpty", "日志集非空。");
        errorCodeMap.put("InvalidParameter.ClsMissingAuthorization", "没有授权信息。");
        errorCodeMap.put("InvalidParameter.ClsMissingContent", "丢失内容。");
        errorCodeMap.put("InvalidParameter.ClsSyntaxError", "语法错误。");
        errorCodeMap.put("InvalidParameter.ClsTopicClosed", "主题已经关闭。");
        errorCodeMap.put("InvalidParameter.ClsTopicConflict", "主题冲突。");
        errorCodeMap.put("InvalidParameter.EcdnInterfaceError", "内部接口错误，请重试或联系客服人员解决。");
        errorCodeMap.put("InvalidParameter.ParamError", "参数错误。");
        errorCodeMap.put("InvalidParameter.PathRegexTooManySubPattern", "正则子模式超出上限。");
        errorCodeMap.put("InvalidParameter.RemoteAuthInvalidPlatform", "域名所在平台不支持远程鉴权。");
        errorCodeMap.put("InvalidParameter.RemoteAuthInvalidProtocol", "域名所在平台不支持使用https协议访问远程鉴权地址。");
        errorCodeMap.put("InvalidParameter.ScdnLogTaskExpired", "任务已过期,无法重试。");
        errorCodeMap.put("InvalidParameter.ScdnLogTaskNotFoundOrNotFail", "任务不存在或任务未失败。");
        errorCodeMap.put("InvalidParameter.ScdnLogTaskTimeRangeInvalid", "时间范围错误。");
        errorCodeMap.put("LimitExceeded.CamResourceArrayTooLong", "资源数组超过最大值。");
        errorCodeMap.put("LimitExceeded.CamResourceTooManyTagKey", "单个资源标签键数不能超过50。");
        errorCodeMap.put("LimitExceeded.CamTagKeyTooLong", "标签键长度超过最大值。");
        errorCodeMap.put("LimitExceeded.CamTagKeyTooManyTagValue", "单个标签键对应标签值不能超过1000。");
        errorCodeMap.put("LimitExceeded.CamTagQuotaExceedLimit", "域名绑定标签数量超出限制。");
        errorCodeMap.put("LimitExceeded.CamUserTooManyTagKey", "单个用户最多1000个不同的key。");
        errorCodeMap.put("LimitExceeded.CdnCallingQueryIpTooOften", "查询IP归属操作过于频繁。");
        errorCodeMap.put("LimitExceeded.CdnClsTooManyTopics", "该账号已经创建了太多主题。");
        errorCodeMap.put("LimitExceeded.CdnConfigTooManyCacheRules", "缓存配置规则数超出限制。");
        errorCodeMap.put("LimitExceeded.CdnHostOpTooOften", "域名操作过于频繁。");
        errorCodeMap.put("LimitExceeded.CdnPurgeExceedBatchLimit", "刷新的Url数量超过每批提交的限制。");
        errorCodeMap.put("LimitExceeded.CdnPurgeExceedDayLimit", "刷新的Url数量超过每日限额。");
        errorCodeMap.put("LimitExceeded.CdnPurgePathExceedBatchLimit", "刷新的目录数量超过限制。");
        errorCodeMap.put("LimitExceeded.CdnPurgePathExceedDayLimit", "刷新的目录数量超过每日限制。");
        errorCodeMap.put("LimitExceeded.CdnPurgeUrlExceedBatchLimit", "刷新的Url数量超过限制。");
        errorCodeMap.put("LimitExceeded.CdnPurgeUrlExceedDayLimit", "刷新的Url数量超过每日限额。");
        errorCodeMap.put("LimitExceeded.CdnPushExceedBatchLimit", "预热的Url数量超过单次限制。");
        errorCodeMap.put("LimitExceeded.CdnPushExceedDayLimit", "预热的Url数量超过每日限制。");
        errorCodeMap.put("LimitExceeded.CdnQueryIpBatchTooMany", "批量查询IP归属个数超过限制。");
        errorCodeMap.put("LimitExceeded.CdnUserTooManyHosts", "用户域名数量已达上限，请联系客服人员处理。");
        errorCodeMap.put("LimitExceeded.ClsLogSizeExceed", "日志大小超限。");
        errorCodeMap.put("LimitExceeded.ClsLogsetExceed", "日志集数目超出。");
        errorCodeMap.put("LimitExceeded.ClsTopicExceed", "主题超限。");
        errorCodeMap.put("LimitExceeded.ScdnLogTaskExceedDayLimit", "每日任务数量超出最大值。");
        errorCodeMap.put("OperationDenied", "操作被拒绝。");
        errorCodeMap.put("OperationDenied.ShareCacheAreaDnsNotMatch", "所选目标域名与当前域名平台不一致，请重新选择或联系客服人员获得技术支持");
        errorCodeMap.put("ResourceInUse.CdnConflictHostExists", "域名与系统中已存在域名存在冲突。");
        errorCodeMap.put("ResourceInUse.CdnHostExists", "域名已存在。");
        errorCodeMap.put("ResourceInUse.CdnOpInProgress", "CDN资源正在被操作中。");
        errorCodeMap.put("ResourceInUse.TcbHostExists", "域名已存在。");
        errorCodeMap.put("ResourceNotFound.CamTagKeyNotExist", "标签键不存在。");
        errorCodeMap.put("ResourceNotFound.CdnHostNotExists", "未查询到该域名，请确认域名是否正确。");
        errorCodeMap.put("ResourceNotFound.CdnProjectNotExists", "项目不存在，请确认后重试。");
        errorCodeMap.put("ResourceNotFound.CdnUserNotExists", "未开通CDN服务，请开通后使用此接口。");
        errorCodeMap.put("ResourceNotFound.CdnUserTooManyHosts", "用户域名数量已达上限，请联系客服人员处理。");
        errorCodeMap.put("ResourceNotFound.ClsIndexNotExist", "索引不存在。");
        errorCodeMap.put("ResourceNotFound.ClsLogsetNotExist", "日志集不存在。");
        errorCodeMap.put("ResourceNotFound.ClsTopicNotExist", "主题不存在。");
        errorCodeMap.put("ResourceNotFound.EcdnDomainNotExists", "域名不存在，请确认后重试。");
        errorCodeMap.put("ResourceUnavailable.CdnHostBelongsToOthersInMainland", "该域名已在其他处接入中国境内服务地域，如需修改服务地域为全球，需验证取回域名。");
        errorCodeMap.put("ResourceUnavailable.CdnHostBelongsToOthersInOverseas", "该域名已在其他处接入中国境外服务地域，如需修改服务地域为全球，需验证取回域名。");
        errorCodeMap.put("ResourceUnavailable.CdnHostExistsInDsa", "域名已接入DSA功能。");
        errorCodeMap.put("ResourceUnavailable.CdnHostExistsInTcb", "域名已经在TCB控制台接入。");
        errorCodeMap.put("ResourceUnavailable.CdnHostIsLocked", "域名被锁定。");
        errorCodeMap.put("ResourceUnavailable.CdnHostIsMalicious", "该域名有违法违规风险，不可接入。");
        errorCodeMap.put("ResourceUnavailable.CdnHostIsNotOffline", "域名未下线。");
        errorCodeMap.put("ResourceUnavailable.CdnHostIsNotOnline", "域名已下线，无法提交预热。");
        errorCodeMap.put("ResourceUnavailable.CdnHostNoIcp", "域名未备案，请将域名备案。备案同步周期为2小时，若域名已备案，可稍后重新接入。");
        errorCodeMap.put("ResourceUnavailable.HostExistInVod", "该域名已在云点播内接入，请先在云点播内删除域名后再接入。");
        errorCodeMap.put("ResourceUnavailable.ScdnUserNoPackage", "SCDN服务未生效，请购买或续费SCDN套餐后重试。");
        errorCodeMap.put("ResourceUnavailable.ScdnUserSuspend", "SCDN服务未生效，请购买或续费SCDN套餐后重试。");
        errorCodeMap.put("UnauthorizedOperation.CdnAccountUnauthorized", "子账号禁止查询整体数据。");
        errorCodeMap.put("UnauthorizedOperation.CdnCamUnauthorized", "子账号未配置cam策略。");
        errorCodeMap.put("UnauthorizedOperation.CdnClsNotRegistered", "该账号未授权开通CLS。");
        errorCodeMap.put("UnauthorizedOperation.CdnDomainRecordNotVerified", "域名解析未进行验证。");
        errorCodeMap.put("UnauthorizedOperation.CdnHostExistsInInternal", "域名在内部系统已存在，请提工单处理。");
        errorCodeMap.put("UnauthorizedOperation.CdnHostInIcpBlacklist", "该域名涉及违法违规风险，不可接入。");
        errorCodeMap.put("UnauthorizedOperation.CdnHostIsOwnedByOther", "该域名属于其他账号，您没有权限接入。");
        errorCodeMap.put("UnauthorizedOperation.CdnHostIsToApplyHost", "域名需要提工单申请接入。");
        errorCodeMap.put("UnauthorizedOperation.CdnHostIsUsedByOther", "域名已被其他账号接入，更多详情请提交工单联系我们。");
        errorCodeMap.put("UnauthorizedOperation.CdnHostUnauthorized", "CDN子账号加速域名未授权。");
        errorCodeMap.put("UnauthorizedOperation.CdnInvalidUserStatus", "用户状态不合法，暂时无法使用服务。");
        errorCodeMap.put("UnauthorizedOperation.CdnProjectUnauthorized", "子账号项目未授权。");
        errorCodeMap.put("UnauthorizedOperation.CdnTagUnauthorized", "子账号标签未授权。");
        errorCodeMap.put("UnauthorizedOperation.CdnTxtRecordValueNotMatch", "域名解析记录值验证不通过。");
        errorCodeMap.put("UnauthorizedOperation.CdnUserAuthFail", "CDN用户认证失败。");
        errorCodeMap.put("UnauthorizedOperation.CdnUserAuthWait", "CDN用户待认证。");
        errorCodeMap.put("UnauthorizedOperation.CdnUserInvalidCredential", "内部服务错误，请联系客服人员进一步排查。");
        errorCodeMap.put("UnauthorizedOperation.CdnUserIsIsolated", "账号由于欠费被隔离，请冲正后重试。");
        errorCodeMap.put("UnauthorizedOperation.CdnUserIsSuspended", "加速服务已停服，请重启加速服务后重试。");
        errorCodeMap.put("UnauthorizedOperation.CdnUserNoWhitelist", "非内测白名单用户，无该功能使用权限。");
        errorCodeMap.put("UnauthorizedOperation.ClsInvalidAuthorization", "无效的授权。");
        errorCodeMap.put("UnauthorizedOperation.ClsServiceNotActivated", "CLS服务未开通，请先在CLS控制台开通服务。");
        errorCodeMap.put("UnauthorizedOperation.ClsUnauthorized", "授权未通过。");
        errorCodeMap.put("UnauthorizedOperation.CsrfError", "内部服务错误，请联系客服人员进一步排查。");
        errorCodeMap.put("UnauthorizedOperation.DomainEmpty", "鉴权域名为空。");
        errorCodeMap.put("UnauthorizedOperation.EcdnMigratedCdn", "请前往CDN控制台进行操作。");
        errorCodeMap.put("UnauthorizedOperation.NoPermission", "未授权的操作。");
        errorCodeMap.put("UnauthorizedOperation.OpNoAuth", "暂不支持此操作，请联系客服人员处理。");
        errorCodeMap.put("UnauthorizedOperation.OperationTooOften", "操作超出调用频次限制。");
        errorCodeMap.put("UnauthorizedOperation.Unknown", "未授权操作。");
        errorCodeMap.put("UnsupportedOperation.ClsNotAllowed", "不允许操作。");
        errorCodeMap.put("UnsupportedOperation.OpNoAuth", "暂不支持此操作，请联系客服人员处理。");
    }

    /**
     * 获取错误描述
     *
     * @param errorCode 错误码
     * @return 错误描述
     */
    public static String getErrorDescription(String errorCode) {
        return errorCodeMap.getOrDefault(errorCode, errorCode);
    }

    public static String getErrorDescription(TencentCloudSDKException e) {
        return errorCodeMap.getOrDefault(e.getErrorCode(), e.getMessage());
    }
}