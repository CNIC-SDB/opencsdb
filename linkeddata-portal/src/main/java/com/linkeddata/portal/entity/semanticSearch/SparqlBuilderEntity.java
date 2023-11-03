package com.linkeddata.portal.entity.semanticSearch;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "同类查询语句对象")
public class SparqlBuilderEntity {
    /**
     * sparql语句
     */
    private List<String> sparqlList;
    /**
     * 起点
     */
    private String start;
    /**
     * 终点
     */
    private String end;
    /**
     * 所属sparql端点
     */
    private String endpoint;
    /**
     * 文件类型
     */
    private String questionType;


}
