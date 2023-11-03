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
public class RSameAs {
    @Field(type = FieldType.Text,index = false)
    private String sameAsLink;
    @Field(type = FieldType.Text,index = false)
    private String sameAsShort;
    @Field(type = FieldType.Nested)
    private Set<RValue> value;
}
