package com.linkeddata.portal.entity.mongo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 常用实体谓语名称-URL对应表 (如：植物：http://rs.tdwg.org/dwc/terms/Taxon)
 *
 * @author 高帅
 * @date 2023-09-13 10:38
 */
@Data
@ApiModel("常用实体谓语名称-URL对应表 (如：类型 type->http://www.w3.org/1999/02/22-rdf-syntax-ns#type)")
@Document(collection = "entityProperty")
public class EntityProperty {

    /**
     * 中文名称
     */
    private String label;
    /**
     * 英文名称
     */
    private String label_en;
    /**
     * 机构
     */
    private String org;
    /**
     * 完整uri
     */
    private String uri;
    /**
     * uri是否可解析
     */
    private String uri_parse;
}
