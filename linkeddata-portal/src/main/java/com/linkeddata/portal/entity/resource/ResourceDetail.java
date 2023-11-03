package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 数据资源详情实体类
 *
 * @author wangzhiliang
 * @date 20220914
 */
@ApiModel("数据资源详情实体类")
@Data
public class ResourceDetail {

    @ApiModelProperty("标题")
    private String title;
    @ApiModelProperty("主语")
    private String subject;
    @ApiModelProperty("主语简写")
    private String subjectShort;
    @ApiModelProperty("发布日期")
    private Date publishDate;
    @ApiModelProperty("机构名称")
    private String unitName;
    @ApiModelProperty("机构链接")
    private String website;
    @ApiModelProperty("数据集名称")
    private String datasetName;
    @ApiModelProperty("标识符")
    private String identifier;
    @ApiModelProperty("dataSetImage")
    private String dataSetImage ;
    @ApiModelProperty("内容")
    private List<ResourceDataSet> dataSets;
}
