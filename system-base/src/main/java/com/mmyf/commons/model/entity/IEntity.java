package com.mmyf.commons.model.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体的父类
 * <p>
 * 用来限制类型做类型安全的
 *
 * @param <K> 主键类型
 * @author Teddy
 * @version 4.0
 */
public interface IEntity<K extends Serializable> extends Serializable {
    /**
     * 获取主键
     *
     * @return 主键 id
     */
    K getId();

    /**
     * 设置主键
     *
     * @param k 主键值
     * @return 实体 k
     */
    void setId(K k);

    /**
     * 设置创建时间
     * @param createTime 创建时间
     */
    void setCreateTime(LocalDateTime createTime);

    /**
     * 获取创建时间
     * @return 创建时间
     */
    LocalDateTime getCreateTime();

    /**
     * 设置修改时间
     * @param modifyTime 修改时间
     */
    void setModifyTime(LocalDateTime modifyTime);

    /**
     * 获取修改时间
     * @return 修改时间
     */
    LocalDateTime getModifyTime();
}
