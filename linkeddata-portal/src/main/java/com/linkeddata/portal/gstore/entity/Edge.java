package com.linkeddata.portal.gstore.entity;

import lombok.Data;

import java.util.Objects;

/**
 * gstore 查询出的边，实体
 *
 * @author : gaoshuai
 * @date : 2023/2/16 12:49
 */
@Data
public class Edge {
    /**
     * 从哪个节点
     */
    private Integer fromNode;
    /**
     * 到哪个节点
     */
    private Integer toNode;
    /**
     * 谓语
     */
    private String predIRI;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return fromNode.equals(edge.fromNode) && toNode.equals(edge.toNode) && predIRI.equals(edge.predIRI);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromNode, toNode, predIRI);
    }
}
