package com.linkeddata.portal.entity;


import lombok.Data;

/**
 * 语言类型实体
 *
 * @author wangzhiliang
 */
@Data
public class EncodeEntity {
    /**
     * unicode 编码区间
     */
    private String reg;
    /**
     * 语言名称
     */
    private String Zname;


    public EncodeEntity(String reg, String zname) {
        this.reg = reg;
        this.Zname = zname;
    }

}