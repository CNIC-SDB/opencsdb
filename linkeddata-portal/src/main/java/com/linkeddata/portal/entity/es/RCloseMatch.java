package com.linkeddata.portal.entity.es;

import lombok.*;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Set;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RCloseMatch {
    @Field(type = FieldType.Text,index = false)
    private String closeMatchLink;
    @Field(type = FieldType.Text,index = false)
    private String closeMatchShort;
    @Field(type = FieldType.Nested)
    private Set<RValue> value;
}
