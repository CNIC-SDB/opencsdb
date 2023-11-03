package com.linkeddata.portal.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 导出请求对象
 *
 * @author wangzhiliang
 * @date 20220930
 */
@Data
public class ExportRequest {
    @ApiModelProperty("导出数据集图名")
    private String exportGraph;
    @ApiModelProperty("导出是数据所在RDF端点")
    private String exportSparql;
    @ApiModelProperty("导出文件存储绝对路径")
    private String exportPath;
    @ApiModelProperty("导出文件名称")
    private String exportFileName;
}
