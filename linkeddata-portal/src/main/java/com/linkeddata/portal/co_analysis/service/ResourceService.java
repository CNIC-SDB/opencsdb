package com.linkeddata.portal.co_analysis.service;

/**
 * @author : gaoshuai
 * @date : 2022/12/26 10:07
 */
public interface ResourceService {

    /**
     * 检索列表接口
     *
     * @param keyword
     * @param dataCenterId
     * @return
     */
    String getResourceList(String keyword, String dataCenterId, Integer pageNum, Integer pageSize);

    /**
     * 根据id检索单个信息接口
     *
     * @param id
     * @return
     */
    String getResourceById(String id);

}
