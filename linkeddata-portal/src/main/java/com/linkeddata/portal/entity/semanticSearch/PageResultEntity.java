package com.linkeddata.portal.entity.semanticSearch;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author 陈锟
 * @date 2023年3月23日18:22:48
 */
@Data
@ApiModel(value = "语义检索列表模式返回对象")
public class PageResultEntity {

    @ApiModelProperty(value = "实体iri")
    private String iri;

    @ApiModelProperty(value = "rdf:label。多值")
    private List<String> label;

    @ApiModelProperty(value = "rdf:type。多值，每个值有label和iri")
    private List<BasicResultEntity> type;

    @ApiModelProperty(value = "关联对象list。多值，每个值有label和iri")
    private List<BasicResultEntity> relationList;

    @ApiModelProperty(value = "主语iri的谓语列表")
    private List<BasicResultEntity> predicateList;

    @ApiModelProperty(value = "所属机构名称")
    private String applicationName;

}
