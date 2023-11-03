package com.linkeddata.portal.entity.semanticSearch;

import lombok.Data;

import java.util.Objects;

/**
 * 问句类型1，结果的查询依据。
 * 用于传递给python代码
 *
 * @author gaoshuai
 * @date 2023/10/05
 */
@Data
public class Document {
    /**
     * 内容文本
     */
    private String page_content;

    /**
     * 数据中心uri，endpoint的url
     */
    private String endPointUrl;

    /**
     * 数据中心名称
     */
    private String dataCenterName;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return page_content.equals(document.page_content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page_content);
    }
}
