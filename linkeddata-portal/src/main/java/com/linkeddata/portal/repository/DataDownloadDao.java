package com.linkeddata.portal.repository;

/**
 * 下载相关
 *
 * @author : gaoshuai
 * @date : 2022/10/25 14:47
 */
public interface DataDownloadDao {

    /**
     * 记录文件下载
     *
     * @param ip           浏览客户端ip
     * @param url          下载文件地址
     * @param fileType     文件类型，dataset:数据集元数据，resource_entity:数据集实体文件
     * @param datacenterId 数据中心id
     */
    void addDownLoadCount(String ip, String url, String fileType, String datacenterId);
}
