package com.linkeddata.portal.service;

import com.linkeddata.portal.entity.DataSetRequest;
import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.Dataset;

import java.util.List;
import java.util.Map;

/**
 * 数据集服务层接口
 *
 * @author : gaoshuai
 * @date : 2022/9/9 9:58
 */
public interface DatasetService {


    /**
     * 查询所有数据集列表
     *
     * @return
     */
    List<Dataset> listDatasets();

    /**
     * 查询所有数据集列表（展示在首页云图中的）
     *
     * @return
     */
    List<Dataset> listShowDatasets();

    /**
     * 查询某一领域的数据集
     *
     * @param domain 领域名称
     * @return
     */
    List<Dataset> listDatasetsByDomain(String domain);

    /**
     * 查询某一数据中心的数据集
     *
     * @param datacenterId 数据中心id
     * @return
     */
    List<Dataset> listDatasetsByDatacenterId(String datacenterId);

    /**
     * 获取数据列表
     *
     * @param request 请求参数实体
     * @return PageTools<List < Dataset>>  分好页的数据
     * @author wangzhiliang
     * @date 20220919
     */
    PageTools<List<Dataset>> getDatasetList(DataSetRequest request);

    /**
     * 根据数据集id查询数据集详情
     *
     * @param id
     * @return
     */
    Dataset getDatasetById(String id);

    /**
     * 根据数据集identifier查询数据集详情
     *
     * @param identifier
     * @return
     */
    Dataset getDatasetByIdentifier(String identifier);

    /**
     * 根据数据集id下载单个数据集
     *
     * @param id
     */
    void downloadOneDataset(String id);

    /**
     * 数据集下载页-批量下载数据集
     *
     * @param idList 数据集id列表
     */
    void downloadDatasetsByIds(List<String> idList);

    /**
     * 查询所有数据集所属机构
     *
     * @return
     */
    List<String> listInstitutions();

    /**
     * 查询所有
     *
     * @return
     */
    List<String> listDomains();

    /**
     * 列出与该数据集相关的数据集id、名称
     *
     * @param id 该数据集id
     * @return
     */
    Map listRelationDataset(String id);

    /**
     * 根据sparql端点查询数据信息
     *
     * @param sparql sparql端点链接
     * @return
     */
    Dataset getDatasetBySparql(String sparql);

    /**
     * 查询所有数据集之间的关联关系
     *
     * @return
     */
    Map listAllDatasetRelation(String datacenterId);

    /**
     * 首页统计信息
     *
     * @return
     */
    Map getStatisticsInfo(String datacenterId);

    /**
     * 获取数据集多有rdf体量，并以接近的单位显示
     *
     * @return
     */
//    String datasetVolume();


    /**
     * 根据virtuoso图名称查询数据集identifier
     *
     * @param graph 图名称
     * @return
     */
    String getIdentifierByGraph(String graph);
}
