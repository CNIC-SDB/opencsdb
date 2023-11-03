package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Set;

/**
 * 数据资源实体 每行数据详情
 *
 * @author wangzzhiliang
 * @@date  20220913
 *
 */
@Data
@ApiModel("数据资源实体每行数据详情")
public class ResourceDetailLine {
    @ApiModelProperty("谓语链接")
    private String predicate ;
    @ApiModelProperty("谓语链接缩写")
    private String shortPre;
    @ApiModelProperty("谓语本体label名称")
    private String preLabel;
    @ApiModelProperty("宾语内容实体")
    private Set<ResourceContent> contents;

}
