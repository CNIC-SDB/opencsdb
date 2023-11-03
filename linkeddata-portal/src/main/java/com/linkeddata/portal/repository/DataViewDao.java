package com.linkeddata.portal.repository;

/**
 * @author : gaoshuai
 * @date : 2022/10/25 11:08
 */
public interface DataViewDao {

    /**
     * 记录访问客户端ip，访问的url
     *
     * @param ip           访问的客户端ip
     * @param url          访问的url
     * @param datacenterId 数据中心id
     */
    void saveViewCount(String ip, String url, String datacenterId);
}
