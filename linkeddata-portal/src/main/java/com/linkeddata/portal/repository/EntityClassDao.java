package com.linkeddata.portal.repository;

import com.linkeddata.portal.entity.mongo.EntityClass;

import java.util.List;

/**
 * @auhor xiajl
 * @date 2023/4/26 16:34
 */
public interface EntityClassDao {
    /**
     * 根据实体名称查找所有的常用实体类
     *
     * @param label 实体名称
     * @return
     */
    List<EntityClass> findByLabel(String label);

    /**
     * 根据实体uri查找所有的常用实体类
     *
     * @param uri
     * @return EntityClass
     */
    EntityClass findByUri(String uri);
}
