package com.kuocai.cdn.service.domain.cacheset;

import cn.hutool.core.date.DateUtil;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksClient;
import com.kuocai.cdn.api.cdnetworks.cdn.dto.CreatePrefetchDTO;
import com.kuocai.cdn.api.cdnetworks.cdn.dto.CreatePurgeDTO;
import com.kuocai.cdn.api.cdnetworks.cdn.dto.QueryPurgeDTO;
import com.kuocai.cdn.api.cdnetworks.cdn.vo.CreatePrefetchVO;
import com.kuocai.cdn.api.cdnetworks.cdn.vo.CreatePurgeVO;
import com.kuocai.cdn.api.cdnetworks.cdn.vo.QueryPurgeVO;
import com.kuocai.cdn.entity.CacheTask;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.cacheset.CacheTaskVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class CdnetworksDomainCacheSettingServiceImpl extends BaseService<CdnDomain> implements ICdnCacheSettingPlatformService {
    @Override
    public String submitCachePreheating(String[] urls) throws BusinessException {
        CreatePrefetchDTO dto = new CreatePrefetchDTO();
        // yyyy-MM-ddHH:mm:ss
        String currentDate = DateFormatUtils.format(new Date(), "yyyy-MM-ddHH:mm:ss");
        dto.setName("预热-" + currentDate);
        List<CreatePrefetchDTO.FileList> fileList = Arrays.stream(urls).map(url -> {
            CreatePrefetchDTO.FileList file = new CreatePrefetchDTO.FileList();
            file.setUrl(url);
            return file;
        }).collect(toList());
        dto.setFileList(fileList);
        try {
            CreatePrefetchVO vo = CdnetworksClient.CreatePrefetch(dto);
            return vo.getItemId();
        } catch (Exception e) {
            log.error("预热缓存失败：{}", e.getMessage());
            throw new BusinessException("预热缓存失败：" + e.getMessage());
        }
    }

    @Override
    public String submitCacheRefresh(String[] urls, String type) throws BusinessException {
        CreatePurgeDTO dto = new CreatePurgeDTO();
        if ("directory".equals(type)) {
            dto.setDirs(Arrays.asList(urls));
        } else {
            dto.setUrls(Arrays.asList(urls));
        }
        try {
            CreatePurgeVO vo = CdnetworksClient.CreatePurge(dto);
            return vo.getItemId();
        } catch (Exception e) {
            log.error("刷新缓存失败：{}", e.getMessage());
            throw new BusinessException("刷新缓存失败：" + e.getMessage());
        }
    }

    @Override
    public void queryTaskInfo(CacheTaskType cacheTaskType, List<CacheTaskVo> results, Map<Long, SysUser> sysUserMap, CacheTask cacheTask) throws CdnHuaweiException {
        String taskId = cacheTask.getTaskId();
        if ("refresh".equals(cacheTaskType.getCode())) {
            QueryPurgeDTO dto = new QueryPurgeDTO();
            dto.setItemId(taskId);
            dto.setPageNo("1");
            dto.setPageSize("10");
            try {
                QueryPurgeVO queryPurgeVO = CdnetworksClient.QueryPurge(dto);
                QueryPurgeVO.ResultDetail[] resultDetail = queryPurgeVO.getResultDetail();
                for (QueryPurgeVO.ResultDetail detail : resultDetail) {
                    SysUser user = sysUserMap.get(cacheTask.getUserId());
                    CacheTaskVo cacheTaskVo = CacheTaskVo.builder()
                            .taskType(cacheTaskType.getName())
                            .url(detail.getUrl())
                            .fileType(2 == detail.getIsDir() ? "目录" : "文件")
                            .createTime(detail.getCreateTime())
                            .createTimeLong(convertTime(detail.getCreateTime()))
                            .status(convertStatus(detail.getStatus()))
                            .userId(user.getId())
                            .userName(user.getUserName())
                            .img(user.getImg())
                            .build();
                    results.add(cacheTaskVo);
                }
            } catch (Exception e) {
                log.error("刷新缓存失败：{}", e.getMessage());
                throw new CdnHuaweiException("查询刷新缓存任务失败：" + e.getMessage());
            }
        } else {
            // todo 预热记录
        }
//        System.out.println(taskId);
    }

    private String convertStatus(String status) {
        // 缓存刷新任务执行的状态，有以下几种状态：success：表示刷新文件缓存的任务执行成功 failure：表示刷新文件缓存的任务执行失败 wait：表示刷新缓存的任务正在排队中 run：表示刷新缓存的任务正在执行中
        switch (status) {
            case "success":
                return "完成";
            case "wait":
            case "run":
                return "处理中";
            default:
                return "失败";
        }
    }

    private Long convertTime(String time) {
        // yyyy-MM-dd HH:mm:ss
        return DateUtil.parse(time).getTime();
    }
}
