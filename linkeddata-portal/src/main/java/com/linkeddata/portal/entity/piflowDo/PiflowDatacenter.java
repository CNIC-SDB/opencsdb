package com.linkeddata.portal.entity.piflowDo;

import com.linkeddata.portal.utils.StringUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;

/**
 * 协同分析映射关系表
 *
 * @author wangzl
 * @date
 */
@ApiModel("协同分析映射关系表")
@Data
@Document(collection = "piflow_datacenter")
public class PiflowDatacenter implements Serializable {
    private static final long serialVersionUID = -8550511504927301027L;
    @Id
    private String id;

    @ApiModelProperty("数据中心")
    private String datacenter;

    @ApiModelProperty("关联网络中的数据中心ID")
    @Field("datacenter_id")
    private String datacenterId;

    @ApiModelProperty("数据中心")
    @Field("piflow_datacenter_id")
    private String piflowDatacenterId;

    @ApiModelProperty("HDFS地址")
    private String hdfs;

    @ApiModelProperty("附件在服务器上的父路径")
    @Field("file_parent_path")
    private String fileParentPath;

}
