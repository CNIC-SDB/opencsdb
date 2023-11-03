package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 关联的数据 实体类
 *
 * @author wangzl
 */
@ApiModel("关联数据集实体")
@Data
public class Link {
    /**
     * 关联的数据集名称
     */
    @ApiModelProperty("关联的数据集identifier")
    private String target;
    /**
     * 关联的数据中相关联的链接数量
     */
    @ApiModelProperty("与关联的数据集相关联的链接数量")
    private String value;

    /**
     * 与数据集相关联的数据集中三元组数量
     */
    @ApiModelProperty("与数据集相关联的数据集中三元组数量")
    @Field("target_triples")
    private Long targetTriples;
}
