package com.kuocai.cdn.dto.datatable;

import lombok.Builder;
import lombok.Data;

/**
 * Datatables字段
 */
@Data
@Builder
public class DataTableColumn {
    private String data;
    private String name;
    private Boolean orderable;
    private DataTableSearch search;
    private Boolean searchable;
}
