package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * 资源实体列表 CloseMatch
 *
 * @author wangzzhiliang
 */
@Data
@ApiModel("skos:closeMatch 内容")
public class ResourceCloseMatch {
    @ApiModelProperty("closeMatch 全链接")
    private String closeMatchLink;
    @ApiModelProperty("closeMatch 全链接简写")
    private String closeMatchShort;
    @ApiModelProperty("值是一个Map  key 存在有链接 不存在是一个字面量")
    private Set<Map<String, String>> value;
}
