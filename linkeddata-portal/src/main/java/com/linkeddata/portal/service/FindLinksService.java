package com.linkeddata.portal.service;


/**
 * @author wangzhiliang
 */
public interface FindLinksService {
    /**
     * 获取dbpedia 得关联关系
     *
     * @author wangzhiliang
     * @date 20221028
     */
    void getPlantLinksByDbpedia();

    /**
     * 获取dbpedia 得关联关系
     *
     * @author wangzhiliang
     * @date 20221028
     */
    void getMircoProteinLinksProteinOntology();

    /**
     * virtuoso 数据库 增加关联关系通用
     *
     * @author wangzhiliang
     * @date 20221201
     */
    void linkedCommon();

    /**
     * virtuoso 数据库 增加label属性的关联关系
     *
     * @param localEndPoint 本地sparql端点
     * @param linkEndPoint  要关联的sparql端点
     * @param filePath      生成的文件位置
     * @author gaoshuai
     * @date 20221201
     */
    void linkedLabelProperty(String localEndPoint, String linkEndPoint, String filePath);

    void linkedGeoNames(String localEndPoint, String linkEndPoint, String filePath);

    /**
     * virtuoso 数据库 增加关联关系通用
     *
     * @author wangzhiliang
     * @date 20221201
     */
    void linkGbif();

    void animalLinkGbif();

    /**
     * virtuoso 删除图
     *
     * @author wangzhiliang
     * @date 20221201
     */
    void deleteGraph();

    /**
     * 化合物数据关联 pubchem 数据
     *
     * @author wangzhiliang
     * @date 20221201
     */
    void dealLinkPubchem();
    /**
     * 化合物数据关联 pubchem dbpedia 数据
     *
     * @author wangzhiliang
     * @date 20221228
     */
    void compoundLink();
    /**
     * 获取 wikidata
     *
     * @author wangzhiliang
     * @date 20221228
     */
    void getWikiData();
    /**
     *  生成临床试验的研究RDF
     *
     * @author wangzhiliang
     * @date 20221228
     */
    void generateResearchRDF();
    /**
     *  生成人口健康rdf
     *
     * @author wangzhiliang
     * @date 20221228
     */
    void generateNCMIRDF();
    /**
     * 生成 化合物跟 mesh 的关系
     *
     * @author wangzhiliang
     * @date 20230215
     */
    void generateByMesh();

}
