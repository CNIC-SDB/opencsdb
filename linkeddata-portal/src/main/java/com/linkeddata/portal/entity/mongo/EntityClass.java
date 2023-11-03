package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 常用实体类名称-URL对应表 (如：植物：http://rs.tdwg.org/dwc/terms/Taxon)
 * @author xiajl
 * @date 2023-04-26 16:23
*/
@Data
@ApiModel("常用实体类名称-URL对应表 (如：植物->http://rs.tdwg.org/dwc/terms/Taxon)")
@Document(collection = "entityClass")
public class EntityClass {
    @Id
    private String id;

    @ApiModelProperty("实体名称")
    @Field("label")
    private String label;

    @ApiModelProperty("uri地址")
    @Field("uri")
    private String uri;

    @ApiModelProperty("端点地址列表")
    @Field("endPointsList")
    private List<String> endPointsList;

}
