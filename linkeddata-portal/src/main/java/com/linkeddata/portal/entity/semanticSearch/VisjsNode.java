package com.linkeddata.portal.entity.semanticSearch;

import lombok.Data;

import java.util.Objects;

/**
 * 对应visjs中的点
 *
 * @author 陈锟
 * @date 2023年3月7日15:54:42
 */
@Data
public class VisjsNode {

    /**
     * 若节点为字面量时，id为宾语String
     */
    private String id;
    private String label;
    // 节点的中文名称
    private String label_zh;
    // 节点的资源类型（植物、药物、化合物等）
    private String type;
    private String applicationName;
    /**
     * 节点是否是iri
     *      iri：true
     *      字面量：false
     */
    private boolean iriFlag = true;
    /**
     * 仅做前端展示用
     */
    private String showIri;

    public VisjsNode() {
    }

    /**
     * 创建未知名称的实体节点时使用
     *
     * @param id
     */
    public VisjsNode(String id, String applicationName) {
        this.id = id;
        this.applicationName = applicationName;
    }

    /**
     * 创建已知名称的实体节点时使用；
     * 创建字面量节点时使用
     *
     * @param id
     */
    public VisjsNode(String id, String label, String applicationName) {
        this.id = id;
        this.label = label;
        this.applicationName = applicationName;
    }

    /**
     * 创建已知名称的实体节点时使用；
     * 创建字面量节点时使用
     *
     * @param id
     * @param label
     * @param applicationName
     * @param iriFlag
     */
    public VisjsNode(String id, String label, String applicationName, boolean iriFlag) {
        this.id = id;
        this.label = label;
        this.applicationName = applicationName;
        this.iriFlag = iriFlag;
    }

    /**
     * 创建已知名称的实体节点时使用；
     * 字段最全
     *
     * @param id
     * @param label
     * @param applicationName
     * @param iriFlag
     * @param showIri
     */
    public VisjsNode(String id, String label, String applicationName, boolean iriFlag, String showIri) {
        this.id = id;
        this.label = label;
        this.applicationName = applicationName;
        this.iriFlag = iriFlag;
        this.showIri = showIri;
    }

    /**
     * 根据 id 去重，同时重写 equals、hashCode 方法
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VisjsNode)) {
            return false;
        }
        VisjsNode other = (VisjsNode) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

