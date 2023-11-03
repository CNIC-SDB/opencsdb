package com.linkeddata.portal.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 数据集列表请求实体
 *
 * @author wangzhiliang
 * @date 20220920
 */
@Data
@ApiModel("数据集列表请求")
@NoArgsConstructor
@Accessors(chain = true)
public class DataSetRequest implements Serializable {
    @ApiModelProperty("所属机构")
    private String[] institution;
    @ApiModelProperty("所属领域")
    private String[] domain;
    @ApiModelProperty("数据中心id")
    private String datacenterId;
    @ApiModelProperty("过滤条件")
    private String condition;
    @ApiModelProperty("当前多少页")
    @NonNull
    private Integer pageNum;
    @ApiModelProperty("每页展示多少条")
    @NonNull
    private Integer pageSize;
}
