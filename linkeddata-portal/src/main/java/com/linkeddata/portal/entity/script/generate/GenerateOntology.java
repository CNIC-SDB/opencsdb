package com.linkeddata.portal.entity.script.generate;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 生成本体参数实体
 *
 * @author wangzhiliang
 * @date 2023/2/20 9:50
 */
@Data
@ApiModel("生成本体参数")
@NoArgsConstructor
@Accessors(chain = true)
public class GenerateOntology {
    /**
     * 本体基础 IRI
     */
    @ApiModelProperty("本体基础 IRI 即 域名地址")
    private String base;
    /**
     * 本体类集合
     */
    @ApiModelProperty("本体类集合")
    private List<String> classList;
    /**
     * 宾语为资源的谓语集合
     */
    @ApiModelProperty("宾语为资源的谓语集合")
    private List<String> objectList;
    /**
     * 宾语为字面量的谓语集合
     */
    @ApiModelProperty("宾语为字面量的谓语集合")
    private List<String>  dataTypeList;
    /**
     * 宾语为字面量的谓语的名称集合
     */
    @ApiModelProperty("宾语为字面量的谓语的名称集合")
    private List<String> dataTypeNameList;
    /**
     * 生成文件地址
     */
    @ApiModelProperty("生成文件名加地址")
    private String filePath;

}
