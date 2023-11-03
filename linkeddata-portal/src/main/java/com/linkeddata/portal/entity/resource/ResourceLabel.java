package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * 资源实体列表 label
 *
 * @author wangzzhiliang
 */
@Data
@ApiModel("rdfs:label 实体")
public class ResourceLabel {
    @ApiModelProperty("label 全链接")
    private String labelLink;
    @ApiModelProperty("label 全链接简写")
    private String labelShort;
    @ApiModelProperty("值 是一个Map  key 存在有链接 不存在是一个字面量")
    private Set<Map<String, String>> value;
}
