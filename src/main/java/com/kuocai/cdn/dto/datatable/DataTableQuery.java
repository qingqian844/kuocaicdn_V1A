package com.kuocai.cdn.dto.datatable;

import lombok.Data;

import java.util.List;

/**
 * Datatables查询条件
 */
@Data
public class DataTableQuery {
    private Integer draw;
    private Integer start;
    private Integer length;
    private DataTableSearch search;
    private List<DataTableColumn> columns;
    private List<DataTableOrder> order;
}