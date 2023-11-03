package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * 资源实体列表 sameAs
 *
 * @author wangzzhiliang
 */
@Data
@ApiModel("owl:sameAs 内容")
public class ResourceSameAs {
    @ApiModelProperty("sameAs 全链接")
    private String sameAsLink;
    @ApiModelProperty("sameAs 全链接简写")
    private String sameAsShort;
    @ApiModelProperty("值是一个Map  key 存在有链接 不存在是一个字面量")
    private Set<Map<String, String>> value;
}
