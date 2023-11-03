package com.linkeddata.portal.Enum;

import lombok.Getter;

import java.util.Objects;

/**
 * RDF格式 文件对后缀
 *
 * @author wangzhiliang
 */
public enum RdfFormatEnum {
    TURTLE("TURTLE", "ttl"),
    RDF("RDF/XML", "rdf"),
    TRIPLE("N-TRIPLE", "nt"),
    JSONLD("JSON-LD", "json");
    /***
     * 传入格式
     * */
    @Getter
    private String skos;
    /**
     * 返回类型
     **/
    @Getter
    private String type;

    RdfFormatEnum(String skos, String type) {
        this.skos = skos;
        this.type = type;
    }
    /**
     * 根据skos 获取type
     * */
    public static String getSkosType(String skos) {
        if (skos == null) {
            return "";
        }
        for (RdfFormatEnum type : RdfFormatEnum.values()) {
            if (Objects.equals(type.getSkos(), skos)) {
                return type.getType();
            }
        }
        return "";
    }
    /**
     * 根据 type 获取skos
     * */
    public static String getSkosByType(String type) {
        if (type == null) {
            return "";
        }
        for (RdfFormatEnum skos : RdfFormatEnum.values()) {
            if (Objects.equals(skos.getType(), type)) {
                return skos.getSkos();
            }
        }
        return "";
    }
}
