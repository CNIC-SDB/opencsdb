package com.linkeddata.portal.entity.semanticSearch;

import lombok.Data;

/**
 * 从问句中识别到的实体
 *
 * @Author 陈锟
 * @Date 2023年3月9日12:35:41
 */
@Data
public class QuestionParseEntity {

    private String name;

    private String iri;

    public QuestionParseEntity() {
    }

    public QuestionParseEntity(String name, String iri) {
        this.name = name;
        this.iri = iri;
    }
}
