package com.linkeddata.portal.entity.semanticSearch;

import lombok.Data;


/**
 * 语义检索Demo，返回table格式的数据
 * 由于对谓语为type的宾语，和非type的宾语查找label,采用截取该Object后半段作为label的方式，
 * 所以一个object的iri只对应一个label，不会像主语的iri从model中可能获得多个lable。
 * 所以建了此类，作为PageResultEntity中的type和relationList
 *
 * @author : gaoshuai
 * @date : 2023/3/28 18:01
 */
@Data
public class BasicResultEntity {
    private String iri;
    private String label;
}
