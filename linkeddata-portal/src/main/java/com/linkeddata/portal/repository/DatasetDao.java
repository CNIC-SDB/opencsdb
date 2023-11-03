package com.linkeddata.portal.repository;

import com.linkeddata.portal.entity.DataSetRequest;
import com.linkeddata.portal.entity.PageTools;
import com.linkeddata.portal.entity.mongo.Dataset;

import java.util.List;

/**
 * 数据查询相关方法接口
 *
 * @author : gaoshuai
 * @date : 2022/9/9 9:58
 */
public interface DatasetDao {
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
     * 查询某一领域数据集
     *
     * @param domain 领域名称
     * @return
     */
    List<Dataset> listDatasetsByDomain(String domain);

    /**
     * 查询某一数据中心数据集
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
     * 根据数据集标识符查询数据集详情
     *
     * @param identifier 数据集标识符
     * @return
     */
    Dataset getDatasetByIdentifier(String identifier);

    /**
     * 根据端点查询数据集信息
     *
     * @param sparql 端点
     * @return
     */
    Dataset getDatasetBySparql(String sparql);

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
     * 查询所有数据集所属领域
     *
     * @return
     */
    List<String> listDomains();

    /**
     * 计算数据集个体数
     *
     * @return
     */
    Integer countDataset();

    /**
     * 计算数据集个体数
     *
     * @return
     */
    Integer countDataset(String datacenterId);

    /**
     * 计算所有数据集的三元组数量
     *
     * @return
     */
    Long countTriples();

    /**
     * 按数据中心计算所有数据集的三元组数量
     *
     * @param datacenterId
     * @return
     */
    Long countTriples(String datacenterId);


    /**
     * rdf 数据体量
     *
     * @return
     */
    Long dataVolume();

    /**
     * 按数据中心统计rdf 数据体量
     *
     * @param datacenterId
     * @return
     */
    Long dataVolume(String datacenterId);

    /**
     * 统计浏览量
     *
     * @return
     */
    Long countView();

    /**
     * 统计某个数据中心的浏览量
     *
     * @param datacenterId
     * @return
     */
    Long countView(String datacenterId);

    /**
     * 下载量
     *
     * @return
     */
    Long countDataDownload();

    /**
     * 统计某个数据中心的下载量
     *
     * @param datacenterId
     * @return
     */
    Long countDataDownload(String datacenterId);

    /**
     * 根据virtuoso图名称查询identifier
     *
     * @param graph
     * @return
     */
    String getIdentifierByGraph(String graph);


    /**
     * 根据标题模糊查询和datacenterId查询数据集分页列表
     * @param title 数据集标题
     * @param datacenterId
     * @param pageNum 页数
     * @param pageSize 每页条数
     * @return
     */
    List<Dataset> listDatasetsPageByTitleAndDatacenterId(String title, String datacenterId, Integer pageNum, Integer pageSize);

    /**
     * 根据标题模糊查询和datacenterId查询数据集的总条数
     * @param title
     * @param datacenterId
     * @return
     */
    Integer countDatasetsByTitleAndDatacenterId(String title, String datacenterId);
}
