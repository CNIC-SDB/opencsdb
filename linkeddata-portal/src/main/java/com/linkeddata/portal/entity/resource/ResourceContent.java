package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 *资源实体内容实体类
 *
 * @author wangzzhiliang
 * @date 20220913
 */
@Data
@ApiModel("资源实体内容实体类")
public class ResourceContent {
    @ApiModelProperty("宾语链接")
    private String iri;
    @ApiModelProperty("宾语链接缩写")
    private String iriShort;
    @ApiModelProperty("宾语本体label")
    private String iriLabel;
    @ApiModelProperty("宾语label")
    private String label;
    @ApiModelProperty("宾语语言")
    private String language;
    @ApiModelProperty("宾语为主语的资源内容")
    private List<ResourceObject> objects;
}
