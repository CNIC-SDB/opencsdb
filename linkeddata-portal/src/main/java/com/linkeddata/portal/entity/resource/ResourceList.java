package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 数据资源列表实体
 *
 * @author wangzhiliang
 */
@Data
@ApiModel("数据资源列表实体")
public class ResourceList {
    @ApiModelProperty("sparql端点")
    private String sparql;
    @ApiModelProperty("机构名称")
    private String unitName;
    @ApiModelProperty("机构链接")
    private String website;
    @ApiModelProperty("数据集名称")
    private String datasetName;
    @ApiModelProperty("数据集标识符")
    private String identifier;
    @ApiModelProperty("标题")
    private String title;
    @ApiModelProperty("主语")
    private String subject;
    @ApiModelProperty("主语简写")
    private String subjectShort;
    @ApiModelProperty("rdf:type")
    private ResourceType type;
    @ApiModelProperty(" rdf:label")
    private ResourceLabel label;
    @ApiModelProperty(" skos:closeMatch")
    private ResourceCloseMatch closeMatch;
    @ApiModelProperty(" owl:sameAs")
    private ResourceSameAs sameAs;

}
