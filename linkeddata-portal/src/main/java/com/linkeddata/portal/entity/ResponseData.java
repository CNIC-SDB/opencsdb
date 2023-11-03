package com.linkeddata.portal.entity;


import com.linkeddata.portal.Enum.EnumCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 返回实体
 *
 * @author wangzzhiliang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "资源实体返回")
public class ResponseData<T> {
    @ApiModelProperty("响应码")
    private Integer code;
    @ApiModelProperty("请求结果")
    private String message;
    @ApiModelProperty("数据内容")
    private T data;

    public void parserEnum(EnumCode enumCode) {
        this.code = enumCode.getCode();
        this.message = enumCode.getMessage();
    }

    /**
     * 定义请求成功的：状态码，描述，结果集数据
     *
     * @param data 传递的响应结果集数据
     * @return 有成功状态码，描述，结果集数据的标准格式对象
     */
    public static <T> ResponseData<T> success(T data) {
        // 创建响应标准格式对象
        ResponseData<T> responseData = new ResponseData<T>();
        // 调用转换器方法，将（成功）枚举常量解析，放入到标准响应数据中。
        responseData.parserEnum(EnumCode.SUCCESS);
        // 放入响应数据
        responseData.setData(data);
        return responseData;
    }

    /**
     * 定义请求失败的：状态码，描述，不包含结果集数据
     *
     * @param enumCode 失败时传递的常见错误枚举常量
     * @return 有失败状态码，描述，没有结果集数据的标准格式对象
     */
    public static <T> ResponseData<T> error(EnumCode enumCode) {
        // 创建响应标准格式对象
        ResponseData<T> responseData = new ResponseData<T>();
        // 调用转换器方法，将（错误）枚举常量解析。
        responseData.parserEnum(enumCode);
        return responseData;
    }

    /**
     * 有成功，有失败，但是失败的状态描述不一定能全部满足需求（枚举类有限），所以，自定义方法实现自定义信息
     *
     * @param code    自定义的状态码
     * @param message 自定义的错误信息
     * @return 有失败自定义状态码，自定义描述，没有结果集数据的标准格式对象
     */
    public static <T> ResponseData<T> generator(int code, String message) {
        // 创建响应标准格式对象
        ResponseData<T> responseData = new ResponseData<T>();
        responseData.setCode(code);
        responseData.setMessage(message);
        return responseData;
    }


}
