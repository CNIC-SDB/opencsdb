package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author chenkun
 * @date 2023年10月18日17:10:12
 */
@Data
@ApiModel("简称表，记录URI和简称之间的对应关系")
@Document(collection = "prefix")
public class Prefix {

    @Id
    private String id;
    /**
     * 简称，例如：chemdb
     */
    private String prefix;
    /**
     * URI，例如：http://chemdb.semweb.csdb.cn/resource/
     */
    private String uri;

}
