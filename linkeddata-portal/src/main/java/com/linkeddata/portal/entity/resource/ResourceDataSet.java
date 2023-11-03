package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 内容分类数据集层面
 * @author wangzhiliang
 * @date 20220914
 */
@Data
@ApiModel("数据集")
public class ResourceDataSet {
    @ApiModelProperty("端点")
    private String sparql;
    @ApiModelProperty("数据集名称")
    private String dataSetName;
    @ApiModelProperty("数据集内容")
    private List<ResourceDetailLine> detailLines;
}
