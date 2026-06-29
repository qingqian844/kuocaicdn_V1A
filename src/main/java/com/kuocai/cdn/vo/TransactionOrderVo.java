package com.kuocai.cdn.vo;

import com.kuocai.cdn.entity.TransactionOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/4/8
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionOrderVo extends TransactionOrder {

    /**
     * 图片路径
     */
    private String imgUrl;
}
