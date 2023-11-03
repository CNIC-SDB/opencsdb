package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 数据集合 实体类
 *
 * @author wangzhiliang
 * @date 20220901
 */
@ApiModel("数据集实体类")
@Data
@Document(collection = "dataset")
public class Dataset {
    /**
     * 数据集id
     */
    @Id
    private String id;
    /**
     * 所属领域，生成svg图用到
     */
    @ApiModelProperty("所属领域")
    private String domain;
    /**
     * 数据集名称，生成svg图用到
     */
    @ApiModelProperty("数据集名称")
    private String title;
    /**
     * 数据集描述，生成svg图用到
     * descript.en 英文描述
     * descript.cn 中文描述
     */
    @ApiModelProperty("数据集描述")
    private Map<String, String> description;
    /**
     * 所属机构名称
     */
    @ApiModelProperty("所属机构名称")
    @Field("unit_name")
    private String unitName;
    /**
     * 所属机构名称
     */
    @ApiModelProperty("所属机构链接")
    @Field("website")
    private String website;
    /**
     * 数据集发布日期
     */
    @ApiModelProperty("数据集发布日期")
    @Field("publish_time")
    private Date publishTime;
    /**
     * 数据集缩略图路径
     */
    @ApiModelProperty("数据集缩略图路径")
    private String image;
    /**
     * 关键词，生成svg图用到
     */
    @ApiModelProperty("关键词")
    private List<String> keywords;
    /**
     * 联系人姓名
     */
    @ApiModelProperty("联系人姓名")
    @Field("contact_name")
    private String contactName;
    /**
     * 联系人邮箱
     */
    @ApiModelProperty("联系人邮箱")
    @Field("contact_email")
    private String contactEmail;
    /**
     * sparql端点链接
     */
    @ApiModelProperty("sparql端点链接")
    private String sparql;
    /**
     * 关联的数据集列表，生成svg图用到
     */
    @ApiModelProperty("关联的数据集列表")
    private List<Link> links;
    /**
     * 标识符，生成svg图用到
     */
    @ApiModelProperty("标识符")
    private String identifier;
    /**
     * 数据集包含三元组数量，生成svg图用到
     */
    @ApiModelProperty("数据集包含三元组数量")
    private Long triples;

    /**
     * 数据集对应实体文件体量，单位 字节
     */
    @ApiModelProperty("数据集对应实体文件体量，单位 字节，压缩包的大小")
    @Field("data_volume")
    private Long dataVolume;

    /**
     * rdf实体文件下载地址
     */
    @ApiModelProperty("rdf实体文件下载地址")
    @Field("rdf_download_url")
    private String rdfDownloadUrl;

    /**
     * 数据中心标识
     */
    @ApiModelProperty("数据中心标识")
    @Field("datacenter_id")
    private String datacenterId;

    //xiajl20230511
    @ApiModelProperty("数据集对应的文件路径(9.6项目中用到)")
    @Field("hdfsPath")
    private String hdfsPath;

}
