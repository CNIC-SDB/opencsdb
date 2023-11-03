package com.linkeddata.portal.entity.semanticSearch;

import lombok.Data;

import java.util.Objects;

/**
 * 对应visjs中的边
 *
 * @author 陈锟
 * @date 2023年3月7日15:54:42
 */
@Data
public class VisjsEdge {

    private String from;
    private String to;
    private String uri;
    private String label;
    private String label_zh;

    public VisjsEdge() {
    }

    public VisjsEdge(String from, String to, String uri) {
        this.from = from;
        this.to = to;
        this.uri = uri;
    }

    /**
     * 创建已知名称的实体节点时使用；
     * 字段最全
     *
     * @param from
     * @param to
     * @param uri
     * @param label
     */
    public VisjsEdge(String from, String to, String uri, String label) {
        this.from = from;
        this.to = to;
        this.uri = uri;
        this.label = label;
    }

    /**
     * 根据 from、to、uri 去重，同时重写 equals、hashCode 方法
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VisjsEdge)) {
            return false;
        }
        VisjsEdge other = (VisjsEdge) obj;
        return Objects.equals(from + "-" + to + "-" + uri, other.from + "-" + other.to + "-" + other.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from + "-" + to + "-" + uri);
    }
}
