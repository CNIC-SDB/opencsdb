package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * 下载量实体
 *
 * @author : gaoshuai
 * @date : 2022/10/24 17:18
 */
@ApiModel("下载量实体类")
@Data
@Document("data_download")
public class DataDownload {
    @Id
    private String id;
    /**
     * 下载客户端ip
     */
    @ApiModelProperty("下载客户端ip")
    private String ip;
    /**
     * 下载文件路径
     */
    @ApiModelProperty("下载文件路径")
    private String url;
    /**
     * 下载文件类型，dataset：数据集元数据，resource_entity： 数据实体
     */
    @ApiModelProperty("下载文件类型，dataset：数据集元数据，resource_entity： 数据实体")
    @Field("file_type")
    private String fileType;

    /**
     * 何时下载
     */
    @ApiModelProperty("下载时间")
    private Date time;

    /**
     * 数据中心标识
     */
    @ApiModelProperty("数据中心标识")
    @Field("datacenter_id")
    private String datacenterId;

}
