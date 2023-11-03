package com.linkeddata.portal.entity.semanticSearch;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 对应visjs中的子图，封装此类的目的是用于分批展示路径，使得路径呈动态效果
 *
 * @author 陈锟
 * @date 2023年3月7日15:54:42
 */
@Data
public class VisjsGroup {

    private Set<VisjsNode> nodes = new HashSet<>();

    private Set<VisjsEdge> edges = new HashSet<>();

}
