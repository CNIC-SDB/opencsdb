package com.linkeddata.portal.co_analysis.repository;

import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.entity.piflowDo.PiflowDatacenter;

import java.util.List;

/**
 * @author : gaoshuai
 * @date : 2022/12/25 12:42
 */
public interface ResourceDao {


    /**
     * 根据piflow数据中心编号查询数据中心编码
     *
     * @param piflowDataCenterId
     * @return
     */
    String getDataCenterId(String piflowDataCenterId);

    /**
     * 根据sparql端点模糊查询数据集
     *
     * @param sparql
     * @return
     */
    List<Dataset> listDatasetLikeSparql(String sparql);

    /**
     * 根据数据中心id查询数据中心信息
     *
     * @param id
     * @return
     */
    PiflowDatacenter getDataCenterById(String id);


}
