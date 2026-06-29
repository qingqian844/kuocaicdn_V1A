package com.kuocai.cdn.service.base;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuocai.cdn.dto.datatable.DataTableColumn;
import com.kuocai.cdn.dto.datatable.DataTableOrder;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.datatable.DataTableSearch;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.VariableNameUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基础服务类
 *
 * @author XUEW
 * @date 下午9:02 2023/2/12
 */
public abstract class BaseService<T> implements IService<T> {

    @Autowired
    protected BaseMapper<T> dao;

    @Override
    public T save(T t) {
        Map<String, Object> map = BeanUtil.beanToMap(t);
        String key = "id";
        if (Assert.isEmpty(map.get(key))) {
            dao.insert(t);
        } else {
            dao.updateById(t);
        }
        Map<String, Object> result = BeanUtil.beanToMap(t);
        return dao.selectById(MapUtil.getLong(result, "id"));
    }

    @Override
    public int deleteById(Serializable id) {
        return dao.deleteById(id);
    }

    public void deleteByUserId(Long userId) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<T> dataList = queryByWrapper(wrapper);
        if (Assert.notEmpty(dataList)) {
            List<Long> ids = dataList.stream().map(t -> (Long) BeanUtil.beanToMap(t).get("id")).collect(Collectors.toList());
            deleteByIds(ids);
        }
    }

    @Override
    public int deleteByIds(Collection<? extends Serializable> ids) {
        return dao.deleteBatchIds(ids);
    }

    @Override
    public T queryById(Serializable id) {
        return dao.selectById(id);
    }

    @Override
    public List<T> queryByIds(Collection<? extends Serializable> ids) {
        return dao.selectBatchIds(ids);
    }

    @Override
    public List<T> queryByWrapper(Wrapper<T> wrapper) {
        return dao.selectList(wrapper);
    }

    @Override
    public List<T> queryAll() {
        return dao.selectList(null);
    }

    @Override
    public List<T> queryByObj(T obj) {
        if (Assert.isEmpty(obj)) {
            return dao.selectList(null);
        }
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        Map<String, Object> bean2Map = BeanUtil.beanToMap(obj);
        for (Map.Entry<String, Object> temp : bean2Map.entrySet()) {
            String key = temp.getKey();
            if (Assert.isEmpty(temp.getValue())) {
                continue;
            }
            wrapper.eq(VariableNameUtil.humpToLine(key), bean2Map.get(key));
        }
        return dao.selectList(wrapper);
    }

    @Override
    public List<T> queryByMap(Map<String, Object> map) {
        return dao.selectByMap(map);
    }

    @Override
    public IPage<T> queryByWrapperPage(Wrapper<T> wrapper, IPage<T> page) {
        return dao.selectPage(page, wrapper);
    }

    @Override
    public IPage<T> queryByObjPage(T obj, IPage<T> page) {
        if (Assert.isEmpty(obj)) {
            return dao.selectPage(page, null);
        }
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        Map<String, Object> bean2Map = BeanUtil.beanToMap(obj);
        for (Map.Entry<String, Object> temp : bean2Map.entrySet()) {
            String key = temp.getKey();
            if (Assert.isEmpty(temp.getValue())) {
                continue;
            }
            wrapper.eq(VariableNameUtil.humpToLine(key), bean2Map.get(key));
        }
        return dao.selectPage(page, wrapper);
    }

    @Override
    public IPage<T> queryByMapPage(Map<String, Object> map, IPage<T> page) {
        if (Assert.isEmpty(map)) {
            return dao.selectPage(page, null);
        }
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        for (Map.Entry<String, Object> temp : map.entrySet()) {
            String key = temp.getKey();
            if (Assert.isEmpty(temp.getValue())) {
                continue;
            }
            wrapper.eq(VariableNameUtil.humpToLine(key), map.get(key));
        }
        return dao.selectPage(page, wrapper);
    }

    /**
     * DataTable查询
     *
     * @param query 查询参数
     * @return 查询结果
     */
    public JSONObject queryForDatatables(DataTableQuery query) {
        JSONObject responseData = new JSONObject();
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        // 组装分页信息
        Page<T> page = new Page<>(query.getStart() / query.getLength() + 1, query.getLength());
        List<DataTableColumn> columns = query.getColumns();
        // 组装过滤条件
        if (Assert.notEmpty(columns)) {
            for (DataTableColumn column : columns) {
                String columnName = column.getData();
                if (Assert.isEmpty(columnName)) {
                    continue;
                }
                if (columnName.contains(".")) {
                    columnName = columnName.substring(columnName.lastIndexOf(".") + 1);
                }
                DataTableSearch search = column.getSearch();
                String searchValue = search.getValue();
                if (Assert.isEmpty(searchValue) || "all".equals(searchValue)) {
                    continue;
                }
                columnName = VariableNameUtil.humpToLine(columnName);
                wrapper.eq(columnName, searchValue);
            }
        }
        // 组装排序条件
        List<DataTableOrder> tableOrders = query.getOrder();
        if (Assert.notEmpty(tableOrders)) {
            DataTableOrder tableOrder = tableOrders.get(0);
            String column = tableOrder.getColumn();
            column = VariableNameUtil.humpToLine(column);
            if (column.contains(".")) {
                column = column.substring(column.lastIndexOf(".") + 1);
            }
            String dir = tableOrder.getDir();
            if ("desc".equals(dir)) {
                wrapper.orderByDesc(column);
            } else {
                wrapper.orderByAsc(column);
            }
        }
        // 组装搜索条件
        DataTableSearch search = query.getSearch();
        if (Assert.notEmpty(search)) {
            List<String> searchColumns = search.getColumns();
            if (Assert.notEmpty(searchColumns)) {
                wrapper.and(i -> {
                    i.like(searchColumns.get(0), search.getValue());
                    if (searchColumns.size() > 1) {
                        for (int j = 1; j < searchColumns.size(); j++) {
                            i.or().like(searchColumns.get(j), search.getValue());
                        }
                    }
                });
            }
        }
        IPage<T> TIPage = queryByWrapperPage(wrapper, page);
        responseData.put("data", TIPage.getRecords());
        responseData.put("recordsTotal", TIPage.getTotal());
        responseData.put("recordsFiltered", TIPage.getTotal());
        return responseData;
    }

    /**
     * DataTable查询
     *
     * @param userId 用户ID
     * @param query  查询参数
     * @return 查询结果
     */
    public JSONObject queryForDatatables(Long userId, DataTableQuery query) {
        return queryForDatatables(userId, query, "user_id");
    }

    /**
     * DataTable查询
     *
     * @param userId  用户ID
     * @param query   查询参数
     * @param userKey 用户主键数据库名称
     * @return 查询结果
     */
    public JSONObject queryForDatatables(Long userId, DataTableQuery query, String userKey) {
        JSONObject responseData = new JSONObject();
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        // 组装分页信息
        Page<T> page = new Page<>(query.getStart() / query.getLength() + 1L, query.getLength());
        wrapper.eq(userKey, userId);
        List<DataTableColumn> columns = query.getColumns();
        // 组装过滤条件
        if (Assert.notEmpty(columns)) {
            for (DataTableColumn column : columns) {
                String columnName = column.getData();
                if (Assert.isEmpty(columnName)) {
                    continue;
                }
                DataTableSearch search = column.getSearch();
                String searchValue = search.getValue();
                if (Assert.isEmpty(searchValue) || "all".equals(searchValue)) {
                    continue;
                }
                columnName = VariableNameUtil.humpToLine(columnName);
                wrapper.eq(columnName, searchValue);
            }
        }
        // 组装排序条件
        List<DataTableOrder> tableOrders = query.getOrder();
        if (Assert.notEmpty(tableOrders)) {
            DataTableOrder tableOrder = tableOrders.get(0);
            String column = tableOrder.getColumn();
            column = VariableNameUtil.humpToLine(column);
            String dir = tableOrder.getDir();
            if ("desc".equals(dir)) {
                wrapper.orderByDesc(column);
            } else {
                wrapper.orderByAsc(column);
            }
        }
        // 组装搜索条件
        DataTableSearch search = query.getSearch();
        if (Assert.notEmpty(search)) {
            List<String> searchColumns = search.getColumns();
            if (Assert.notEmpty(searchColumns)) {
                wrapper.and(i -> {
                    i.like(searchColumns.get(0), search.getValue());
                    if (searchColumns.size() > 1) {
                        for (int j = 1; j < searchColumns.size(); j++) {
                            i.or().like(searchColumns.get(j), search.getValue());
                        }
                    }
                });
            }
        }
        IPage<T> TIPage = queryByWrapperPage(wrapper, page);
        responseData.put("data", TIPage.getRecords());
        responseData.put("recordsTotal", TIPage.getTotal());
        responseData.put("recordsFiltered", TIPage.getTotal());
        return responseData;
    }

    /**
     * description: 查询count
     *
     * @param wrapper w
     * @return java.lang.Integer
     * @author bo
     * @date 2023/3/20 9:26 AM
     */
    public Long countByWrapper(Wrapper<T> wrapper) {
        return dao.selectCount(wrapper);
    }

    /**
     * 查询数量
     *
     * @param status 状态
     */
    public Long countByStatus(String status) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", status);
        return dao.selectCount(queryWrapper);
    }

    public int updateObjByIds(List<Long> ids, T param) {
        UpdateWrapper<T> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", ids);
        return dao.update(param, updateWrapper);
    }
}
