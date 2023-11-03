package com.linkeddata.portal.entity.semanticSearch;

import com.alibaba.fastjson2.JSONArray;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.*;

/**
 * @author 陈锟
 * @date 2023年3月7日15:54:42
 */
@Data
@ApiModel(value = "语义检索返回结果对象")
public class SemanticSearchResult {

    /**************************************** 前端用 ****************************************/

    @ApiModelProperty(value = "关系图中需要分批展示的点和边")
    private List<VisjsGroup> visjsGroups = new ArrayList<>();

    @ApiModelProperty(value = "语义检索答案总结")
    private String answer;

    @ApiModelProperty(value = "格式同 nodes，问句中的起点，已包含在 visjsGroups 中，不包含字面量")
    private List<VisjsNode> startNodes = new ArrayList<>();

    @ApiModelProperty(value = "格式同 nodes，问句中的终点，已包含在 visjsGroups 中，不包含字面量")
    private List<VisjsNode> endNodes = new ArrayList<>();

    @ApiModelProperty(value = "Node 数量")
    private long nodesNum;

    @ApiModelProperty(value = "是否有列表页。不是所有模板有列表页")
    private boolean hasList;

    @ApiModelProperty(value = "列表页，一次全部返回，前端做分页")
    private List<PageResultEntity> pageResultEntityList;

    @ApiModelProperty(value = "列表页，所有主语的所有谓语列表")
    private List<BasicResultEntity> precateList;

    @ApiModelProperty(value = "只针对问句类型1，键：属性名称，值：多个属性值。对属性进行筛选，属性值包含中文，长度不大于10，每次请求取前3个。返回结果举例：[{'名称':['地不容', '金不换']}, {'类型':['植物']}, ...]")
    private List<Map<String, List<String>>> propertyList;

    /**************************************** 前端不用 ****************************************/

    /**
     * 检索类型。路径图：graph；列表页：list。默认值：graph
     */
    private String queryType;

    /**
     * SPARQL端点list，用于联邦查询
     */
    private String endpoints;

    /**
     * 问句中抽取到的实体 x 的名称
     */
    private String xName;

    /**
     * 问句中抽取到的实体 y 的名称
     */
    private String yName;

    /**
     * 问句中抽取到的实体 x
     */
    private List<QuestionParseEntity> xList;

    /**
     * 问句中抽取到的实体 y
     */
    private List<QuestionParseEntity> yList;

    /**
     * 问句中的起点，对应Node ID
     */
    private List<String> startEntitys = new ArrayList<>();

    /**
     * 问句中的终点，对应Node ID，包含字面量
     */
    private List<String> endEntitys = new ArrayList<>();

    /**
     * 关系图中的点，临时用，返回前会转存到visjsGroups中
     */
    private Set<VisjsNode> nodes = new HashSet<>();

    /**
     * 关系图中的边，临时用，返回前会转存到visjsGroups中
     */
    private Set<VisjsEdge> edges = new HashSet<>();

    /**
     * 关系图中所有实体的label，临时用，返回前会转存到对应的nodes和edges中；
     * 实体：键为IRI，值为label；
     * 字面量：不存
     */
    private Map<String, String> iriLabelMap = new HashMap<>();

    /**
     * 问句类型1，将rdf转换为文本，拼接问句给llm，将拼接后的问句返回，在
     * 页面控制台输出，方便调试
     * 2023/09/21
     * gaoshuai
     */
    private String info;

    /**
     * 问句类型1，llm查询的依据
     * 2020/09/29
     * gaoshuai
     */
    private JSONArray vectorContext;
}
