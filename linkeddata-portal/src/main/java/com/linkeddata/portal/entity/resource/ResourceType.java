package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * 标题对象
 *
 * @author wangzhiliang
 */
@Data
@ApiModel("rdf:type 实体")
public class ResourceType {
    @ApiModelProperty("type 全链接")
    private String typeLink;
    @ApiModelProperty("type 全链接 简写")
    private String typeShort;
    @ApiModelProperty("值 是一个Map  key 存在有链接 不存在是一个字面量")
    private Set<Map<String, String>> value;
}
