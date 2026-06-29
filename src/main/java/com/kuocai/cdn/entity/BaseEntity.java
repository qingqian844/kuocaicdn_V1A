package com.kuocai.cdn.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.Date;

/**
 * 实体公共类
 * 继承此类的子类最好加上 @EqualsAndHashCode 注解,且子类不需要在实现 Serializable
 *
 * @author bo
 * @date 2023/2/13 9:47 AM
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseEntity implements Serializable {

    /**
     * 公共ID属性
     */
    private Long id;

    /**
     * 公共创建时间属性
     */
    private Date createTime;

    /**
     * 公共更新时间属性
     */
    private Date updateTime;

    /**
     * 公共是否删除字段
     */
    private Integer deleted;

}
