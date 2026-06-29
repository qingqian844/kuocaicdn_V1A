package com.kuocai.cdn.service.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 基础服务抽象接口
 *
 * @author XUEW
 * @date 下午9:02 2023/2/12
 */
public interface IService<T> {

    /**
     * 保存数据，包括新增和修改（判断ID）
     *
     * @param t 保存目标
     * @return 保存结果
     */
    T save(T t);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Serializable id);

    /**
     * 通过主键集合删除数据
     *
     * @param ids 主键集合
     * @return 影响行数
     */
    int deleteByIds(Collection<? extends Serializable> ids);

    /**
     * 通过主键查询
     *
     * @param id 主键
     * @return 结果
     */
    T queryById(Serializable id);

    /**
     * 根据主键查询
     *
     * @param ids 主键集合
     * @return 结果列表
     */
    List<T> queryByIds(Collection<? extends Serializable> ids);

    /**
     * 根据查询条件查询
     *
     * @param wrapper 查询条件
     * @return 结果列表
     */
    List<T> queryByWrapper(Wrapper<T> wrapper);

    /**
     * 查询全部
     *
     * @return 结果列表
     */
    List<T> queryAll();

    /**
     * 根据实体查询数据集合
     *
     * @param obj 实体对象
     * @return 结果列表
     */
    List<T> queryByObj(T obj);

    /**
     * 根据实体查询数据集合
     *
     * @param map 字段映射
     * @return 结果列表
     */
    List<T> queryByMap(Map<String, Object> map);

    /**
     * 根据实体分页查询数据集合
     *
     * @param wrapper 查询条件
     * @param page    分页对象
     * @return 结果
     */
    IPage<T> queryByWrapperPage(Wrapper<T> wrapper, IPage<T> page);

    /**
     * 根据实体分页查询数据集合
     *
     * @param obj  实体对象
     * @param page 分页对象
     * @return 结果
     */
    IPage<T> queryByObjPage(T obj, IPage<T> page);

    /**
     * 根据实体分页查询数据集合
     *
     * @param map  字段映射
     * @param page 分页对象
     * @return 结果
     */
    IPage<T> queryByMapPage(Map<String, Object> map, IPage<T> page);
}
