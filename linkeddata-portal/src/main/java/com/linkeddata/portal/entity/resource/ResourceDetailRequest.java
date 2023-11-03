package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 资源实体详情请求参数实体
 *
 * @author wangzhiliang
 * @date 20220915
 */
@Data
@ApiModel("资源详情请求")
public class ResourceDetailRequest {
    @ApiModelProperty("资源主语")
    private String subject;
}
