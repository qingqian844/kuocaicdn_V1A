package com.kuocai.cdn.dto.datatable;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Datatables搜索条件
 */
@Data
@Builder
public class DataTableSearch {
    private List<String> columns;
    private String value;
    private Boolean regex;
}
