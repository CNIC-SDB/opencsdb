package com.linkeddata.portal.entity;

import lombok.Data;

import java.util.List;

/**
 * 让大模型总结问句类型3的结果，接收前端请求
 *
 * @author chenkun
 * @since 2023年9月28日16:24:00
 */
@Data
public class QueryResultForQuestion3Request {

    // 用户原始问句，例如：'对千金藤素有影响的植物都有哪些？'
    private String question;

    // 问句中抽取到的实体和类，例如：'千金藤素'，'植物'
    private String x;
    private String y;

    // 路径中涉及到的类型list，例如：['药材', '植物', '病毒', '化合物', '药物', '临床试验']
    private List<String> classList;

    // 结果实体个数，例如：2
    private Long answerEntityNum;

    // 结果实体list，例如：['地不容', '千金藤']
    private List<String> answerEntityList;

    // 关系图中所有关系
    private List<MyTriple> triples;

}
