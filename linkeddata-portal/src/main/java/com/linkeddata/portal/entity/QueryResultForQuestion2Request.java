package com.linkeddata.portal.entity;

import lombok.Data;

import java.util.List;

/**
 * 让大模型总结问句类型2的结果，接收前端请求
 *
 * @author chenkun
 * @since 2023年9月28日16:24:00
 */
@Data
public class QueryResultForQuestion2Request {

    // 用户原始问句，例如：'地不容和COVID-19有无关联？'
    private String question;

    // 问句中抽取到的实体，例如：'地不容'，'COVID-19'
    private String x;
    private String y;

    // 路径中涉及到的类型list，例如：['药材', '植物', '病毒', '化合物', '药物', '临床试验']
    private List<String> classList;

    // 路径条数，例如：2
    private Long pathNum;

    // 关系图中所有关系
    private List<MyTriple> triples;

}
