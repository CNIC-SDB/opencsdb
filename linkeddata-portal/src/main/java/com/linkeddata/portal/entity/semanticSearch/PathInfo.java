package com.linkeddata.portal.entity.semanticSearch;

import com.linkeddata.portal.entity.neo4j.Relation;
import lombok.Data;

import java.util.List;

/**
 * 从neo4j查询出的路径，添加起点iri和终点iri后返回前端
 *
 * @author : gaoshuai
 * @date : 2023/4/23 13:33
 */
@Data
public class PathInfo {
    /**
     * 起点实体iri
     */
    private String startIri;
    /**
     * 终点实体iri
     */
    private String endIri;

    /**
     * 终点类的 URI
     */
    private String endClassIri;
    /**
     * neo4j中查询出的路径
     */
    private List<Relation> path;

    /**
     * 原始问句中的起点实体名称
     */
    private String startName;
    /**
     * 原始问句中的终点实体名称
     */
    private String endName;
}
