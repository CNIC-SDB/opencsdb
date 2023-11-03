package com.linkeddata.portal.entity;

import lombok.Data;

/**
 * 三元组对象
 *
 * @author chenkun
 * @since 2023年9月28日16:36:23
 */
@Data
public class MyTriple {

    // 主语（IRI）
    private String s;

    // 谓语（IRI）
    private String p;

    // 宾语（IRI或值）
    private String o;

    // 主语的type（IRI）
    private String sType;

    // 宾语的type（若宾语为资源则为IRI，若宾语为字面量则为空）
    private String oType;

    // 机构名称
    private String applicationName;

}
