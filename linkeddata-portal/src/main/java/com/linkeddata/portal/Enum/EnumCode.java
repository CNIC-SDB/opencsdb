package com.linkeddata.portal.Enum;

/**
 * 响应码枚举类
 *
 * @author wangzhiliang
 */

public enum EnumCode {
    // 定义成功的枚举常量，状态码，和描述
    // 这里的代码相当于：public static  final DataEnumCode SUCCESS = new DataEnumCode(0,“ok”)调用类有参构造传值
    SUCCESS(200, "success");


    // 定义的枚举常量属性。
    /**
     * 状态码
     */
    private int code;
    /**
     * 描述
     */
    private String message;

    /**
     * 私有构造,防止被外部调用
     */
    private EnumCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 定义方法,返回描述,跟常规类的定义get没区别
     *
     * @return
     */
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }


}
