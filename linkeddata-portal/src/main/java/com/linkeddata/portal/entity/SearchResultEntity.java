package com.linkeddata.portal.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语义检索结果的来源信息实体
 * @auhor xiajl
 * @date 2023/7/25 16:18
 */
@Data
@NoArgsConstructor
public class SearchResultEntity {
    /**
     * 来源信息
     */
    private String message;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点访问网址
     */
    private String linkAddress;
}
