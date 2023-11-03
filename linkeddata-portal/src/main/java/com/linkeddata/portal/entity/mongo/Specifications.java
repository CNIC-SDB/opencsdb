package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 词表-实体类
 * @author hanmenghang
 * @date : 2022/11/02
 */
@ApiModel("词表-实体类")
@Data
@Document(collection = "specifications")
public class Specifications {
    @Id
    private String id;
    /**
     * 词表名称
     */
    @ApiModelProperty("词表名称")
    private String title;
    /**
     * 词表url地址
     */
    @ApiModelProperty("词表url地址")
    @Field("title_url")
    private String titleUrl;
    /**
     * 词表系统html页面地址
     */
    @ApiModelProperty("词表系统html页面地址")
    @Field("vocabularyPageUrl")
    private String vocabularyPageUrl;
    /**
     * 机构名称
     */
    @ApiModelProperty("机构名称")
    private String institution;
    /**
     * 机构url地址
     */
    @ApiModelProperty("机构url地址")
    @Field("institution_url")
    private String institutionUrl;
    /**
     * 词表描述
     */
    @ApiModelProperty("词表描述")
    private String description;
    /**
     * 词汇量
     */
    @ApiModelProperty("词汇量")
    @Field("vocabulary_size")
    private String vocabularySize;
    /**
     * cstr
     */
    @ApiModelProperty("cstr")
    private String cstr;
    /**
     * doi
     */
    @ApiModelProperty("doi")
    private String doi;
    /**
     * SKOS下载-JSON-LD下载链接
     */
    @ApiModelProperty("SKOS下载-JSON-LD下载链接")
    @Field("download_jsonld")
    private String downloadJsonld;
    /**
     * SKOS下载-N-Triples下载链接
     */
    @ApiModelProperty("SKOS下载-N-Triples下载链接")
    @Field("download_nt")
    private String downloadNt;
    /**
     * SKOS下载-RDF/XML下载链接
     */
    @ApiModelProperty("SKOS下载-RDF/XML下载链接")
    @Field("download_rdf")
    private String downloadRdf;
    /**
     * SKOS下载-Turtle下载链接
     */
    @ApiModelProperty("SKOS下载-Turtle下载链接")
    @Field("download_ttl")
    private String downloadTtl;
}
