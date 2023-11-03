package com.linkeddata.portal.service;

import com.linkeddata.portal.entity.semanticSearch.PathInfo;

import java.util.List;
import java.util.Map;

/**
 * 查询实体间路径相关方法
 *
 * @author : gaoshuai
 * @date : 2023/4/23 10:30
 */
public interface FindPathService {

    /**
     * 根据实体名称，查询实体的type值
     *
     * @param entityName 实体名称
     * @param endpoints  实体可能所属端点 http://xtipc.semweb.csdb.cn/sparql,https://www.plantplus.cn/plantsw/sparql
     * @return [{endpoint=http://xtipc.semweb.csdb.cn/sparql, type=http://purl.obolibrary.org/obo/CHEBI_23888},
     * {endpoint=https://www.plantplus.cn/plantsw/sparql, type=http://rs.tdwg.org/dwc/terms/Taxon}]
     */
    List<Map<String, String>> getEntityType(String entityName, String endpoints);

    /**
     * 问句类型3：对X有影响的Y都有哪些？，y是一种植物或者其他，获取y的type值。
     *
     * @param className
     * @param endpoints
     * @return
     */
    List<Map<String, String>> getClassType(String className, String endpoints);

    /**
     * 获取路径信息，并封装
     *
     * @param xEntityTypeList 起点实体所属type、端点信息，因为同一个实体可能在不同端点中有不同type，所以返回list
     * @param yEntityTypeList 终点实体所属type、端点信息，因为同一个实体可能在不同端点中有不同type，所以返回list
     * @param distince        路径长度
     * @param startName       起点名称
     * @param endName         终点名称
     * @return [PathInfo(startIri = http : / / purl.obolibrary.org / obo / CHEBI_23888, endIri = http : / / rs.tdwg.org / dwc / terms / Taxon, path = [ { } ])
     * PathInfo(startIri=http://rs.tdwg.org/dwc/terms/Taxon, endIri=http://purl.obolibrary.org/obo/CHEBI_23888, path=[{}])
     */
    List<PathInfo> getPathInfo(List<Map<String, String>> xEntityTypeList, List<Map<String, String>> yEntityTypeList, List<String> endpointList, String distince, String startName, String endName);


}
