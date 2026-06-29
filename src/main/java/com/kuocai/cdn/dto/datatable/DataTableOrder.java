package com.kuocai.cdn.dto.datatable;

import lombok.Data;

/**
 * Datatables排序
 */
@Data
public class DataTableOrder {
    private String column;
    private String dir;
}
