package com.linkeddata.portal.entity;

import lombok.Data;

/**
 * @author wangzhiliang
 */
@Data
public class RecordEntity {
    private int count;
    private String Zname;

    public RecordEntity(int count, String zname) {
        this.count = count;
        this.Zname = zname;
    }
}
