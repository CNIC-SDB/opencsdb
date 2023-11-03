package com.linkeddata.portal.entity.semanticSearch;

import lombok.Data;

/**
 * 问句解析结果
 */
@Data
public class QuestionParseResult {

    /**
     * 问句类型：
     * 0：不满足任一问句类型；
     * 1：问句类型1；
     * 2：问句类型2
     */
    private String type;

    /**
     * 问句中第1个实体名称
     */
    private String x;

    /**
     * 问句中第2个实体名称
     */
    private String y;

    /**
     * xiajl20230719 如果是问句类型5，则返回问句中的子问句，供再次查询使用
     */
    private String newQuestion;

    /**
     * xiajl20230721 如果是问句类型6，则需要记录余下的后半部份子问句，以供拼接后再在大模型中查询
     */
    private String remainingQuestion;
}
