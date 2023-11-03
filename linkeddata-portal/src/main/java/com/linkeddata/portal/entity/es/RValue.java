package com.linkeddata.portal.entity.es;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Setting(settingPath = "analyzerSetting.json")
public class RValue {
    @Field(type = FieldType.Text,index = false)
    private String key;
    @Field(type = FieldType.Text,analyzer = "underline_analyzer")
    private String value;
}
