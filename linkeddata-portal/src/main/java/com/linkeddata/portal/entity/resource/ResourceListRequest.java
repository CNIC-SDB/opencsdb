package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 列表请求对象
 *
 * @author wangzhiliang
 */
@Data
@ApiModel("列表请求")
@NoArgsConstructor
@Accessors(chain = true)
public class ResourceListRequest implements Serializable {
    @ApiModelProperty("所属机构")
    private String[] institution;
    @ApiModelProperty("所属领域")
    private String[] domain;
    @ApiModelProperty("过滤条件")
    private String condition;
    @ApiModelProperty("数据集id")
    private String datasetId;
    @ApiModelProperty("es标记  true ")
    private String esFlag;
    @ApiModelProperty("当前多少页")
    @NonNull
    private Integer pageNum;
    @ApiModelProperty("每页展示多少条")
    @NonNull
    private Integer pageSize;
    @ApiModelProperty("门户标记")
    private String  datacenterId;
}
