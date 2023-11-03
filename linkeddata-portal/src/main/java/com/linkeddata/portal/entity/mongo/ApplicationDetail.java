package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 二级门户详情-实体类
 * @author hanmenghang
 * @date : 2022/11/02
 */
@ApiModel("二级门户-实体类")
@Data
@Document(collection = "applications")
public class ApplicationDetail extends  Applications{
    //为详情页增加两个字段，来自dataset表
    /**
     * 领域联系人邮箱地址
     */
    @ApiModelProperty("领域联系人邮箱地址")
    private String email;
    /**
     * 领域联系人
     */
    @ApiModelProperty("领域联系人")
    private String person;
}
