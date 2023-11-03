package com.linkeddata.portal.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语义检索表格数据实体信息
 * @auhor xiajl
 * @date 2023/7/25 17:02
 */
@Data
@NoArgsConstructor
public class TableDataEntity {
    /**
     * 谓语
     */
    private String property;
    /**
     * 谓语链接地址
     */
    private String propertyLink;
    /**
     * 主语或宾语
     */
    private String subject;
    /**
     * 主语或宾语链接
     */
    private String subjectLink;
    /**
     * 来源
     */
    private String provenance;
    /**
     * 来源链接
     */
    private String provenanceLink;
}
