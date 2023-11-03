package com.linkeddata.portal.entity.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 *
 *
 * @author wangzhiliang
 */
@Data
@ApiModel("宾语为资源的实体信息")
public class ResourceObject {
    @ApiModelProperty("宾语是资源全连接")
    private String objectIsIri;
    @ApiModelProperty("宾语是资源缩写")
    private String objectIsIriShort;
    @ApiModelProperty("宾语是资源的label")
    private String objectIsIriLabel;
    @ApiModelProperty("谓语全连接")
    private String objectPre;
    @ApiModelProperty("谓语缩写")
    private String objectPreShort;
    @ApiModelProperty("谓语本体label")
    private String objectPreLabel;
    @ApiModelProperty("宾语资源label")
    private String objectLabel;
    @ApiModelProperty("宾语资源labelLang")
    private String objectLabelLang;
}
