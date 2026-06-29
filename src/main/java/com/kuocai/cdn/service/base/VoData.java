package com.kuocai.cdn.service.base;

import java.util.List;

/**
 * Vo数据转换
 *
 * @param <O>
 * @param <VO>
 */
public interface VoData<O, VO> {

    /**
     * 数据转换
     */
    List<VO> convert2Vo(List<O> source);
}
