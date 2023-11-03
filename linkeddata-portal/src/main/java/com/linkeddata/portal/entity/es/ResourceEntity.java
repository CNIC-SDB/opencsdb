package com.linkeddata.portal.entity.es;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Setting(settingPath = "analyzerSetting.json")
@Document(indexName = "resource_entity_index")
public class ResourceEntity {
    @ApiModelProperty("sparql端点")
    @Field(type = FieldType.Text,index = false)
    private String sparql;
    @ApiModelProperty("机构名称")
    @Field(type = FieldType.Text)
    private String unitName;
    @ApiModelProperty("机构链接")
    @Field(type = FieldType.Text,index = false)
    private String website;
    @ApiModelProperty("数据集名称")
    @Field(type = FieldType.Text)
    private String datasetName;
    @ApiModelProperty("数据集标识符")
    @Field(type = FieldType.Keyword)
    private String identifier;
    @ApiModelProperty("标题")
    @Field(type = FieldType.Text)
    private String title;
    @ApiModelProperty("主语")
    @Field(type = FieldType.Text,index = false)
    private String subject;
    @ApiModelProperty("主语简写")
    //,searchAnalyzer = "underline_analyzer"
    @Field(type = FieldType.Text,analyzer = "underline_analyzer")
    private String subjectShort;
    @ApiModelProperty("rdf:type")
    @Field(type = FieldType.Object)
    private RType type;
    @ApiModelProperty("rdf:label")
    @Field(type = FieldType.Object)
    private RLabel label;
    @ApiModelProperty("skos:closeMatch")
    @Field(type = FieldType.Object)
    private RCloseMatch closeMatch;
    @ApiModelProperty(" owl:sameAs")
    @Field(type = FieldType.Object)
    private RSameAs sameAs;
}
