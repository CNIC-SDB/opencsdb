package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 二级门户-实体类
 * @author hanmenghang
 * @date : 2022/11/02
 */
@ApiModel("二级门户-实体类")
@Data
@Document(collection = "applications")
public class Applications {
    @Id
    private String id;
    /**
     * 门户名称
     */
    @ApiModelProperty("门户名称")
    private String title;
    /**
     * 门户url地址
     */
    @ApiModelProperty("门户url地址")
    @Field("title_url")
    private String titleUrl;
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
     * 二级门户描述
     */
    @ApiModelProperty("二级门户描述")
    private String description;
    /**
     * 图片url地址
     */
    @ApiModelProperty("图片url地址")
    @Field("image_url")
    private String imageUrl;


    /**
     * 数据中心标识
     */
    @ApiModelProperty("数据中心标识")
    @Field("datacenter_id")
    private String datacenterId;

    /**
     * 数据中心基础URL，用于拼接命名空间、SAPRQL端点等
     */
    @ApiModelProperty("数据中心基础URL")
    @Field("baseUrl")
    private String baseUrl;

    /**
     * 该领域资源实体（主语）数量，
     */
    @Field("records")
    private Long records;
}
