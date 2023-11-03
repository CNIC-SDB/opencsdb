package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * 浏览量实体类
 *
 * @author : gaoshuai
 * @date : 2022/10/24 9:53
 */
@ApiModel("浏览量实体类")
@Data
@Document(collection = "data_view")
public class DataView {
    @Id
    private String id;
    /**
     * 浏览页面地址
     */
    @ApiModelProperty("浏览页面地址")
    private String url;
    /**
     * 浏览人ip
     */
    @ApiModelProperty("ip")
    private String ip;
    /**
     * 何时浏览
     */
    @ApiModelProperty("浏览时间")
    private Date time;

    /**
     * 数据中心标识
     */
    @ApiModelProperty("数据中心标识")
    @Field("datacenter_id")
    private String datacenterId;
}
