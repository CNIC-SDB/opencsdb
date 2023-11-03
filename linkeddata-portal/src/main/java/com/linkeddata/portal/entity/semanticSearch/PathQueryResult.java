package com.linkeddata.portal.entity.semanticSearch;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Set;

/**
 * @author 陈锟
 * @date 2023年4月23日13:56:49
 */
@Data
@ApiModel(value = "接口‘根据每个路径对象查询点和边’对应的返回结果，对应接口：queryOnePathForQuestion2、queryOnePathForQuestion3")
public class PathQueryResult {

    @ApiModelProperty(value = "关系图中的点list")
    private Set<VisjsNode> nodes;

    @ApiModelProperty(value = "关系图中的边list")
    private Set<VisjsEdge> edges;

    @ApiModelProperty(value = "关系图中的终点list，用于列表展示，仅在问句类型3中有，对x有影响的y有哪些")
    private Set<String> ends;

    @ApiModelProperty(value = "关系图中的起点实体IRI，用于路径筛选，有且只有1个，但多个请求的起点不一定相同")
    private String startIri;

    @ApiModelProperty(value = "关系图中的终点实体IRI，用于路径筛选，可能有多个")
    private Set<String> endIri;

    @ApiModelProperty(value = "类型list")
    private Set<String> classList;

}
