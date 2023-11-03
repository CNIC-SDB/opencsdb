package com.linkeddata.portal.entity.neo4j;

import lombok.Getter;
import lombok.Setter;

/**
 * 路径实体类
 *
 * @author jinbao
 * @since 2023/4/23
 */
@Getter
@Setter
public class Relation {
    /**
     * 起始节点
     */
    private String start;

    /**
     * 结束节点
     */
    private String end;

    /**
     * 关系名称
     */
    private String relation;

    /**
     * 所属端点
     */
    private String sparqlURI;


    public Relation(String start, String end, String relation, String sparqlURI) {
        this.start = start;
        this.end = end;
        this.relation = relation;
        this.sparqlURI = sparqlURI;
    }
}
