package com.linkeddata.portal.service.impl;

import com.linkeddata.portal.entity.mongo.EntityClass;
import com.linkeddata.portal.entity.neo4j.Relation;
import com.linkeddata.portal.entity.semanticSearch.PathInfo;
import com.linkeddata.portal.repository.EntityClassDao;
import com.linkeddata.portal.service.FindPathService;
import com.linkeddata.portal.utils.RdfUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ResultSet;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询实体间路径相关方法
 *
 * @author : gaoshuai
 * @date : 2023/4/23 10:37
 */
@Service
public class FindPathServiceImpl implements FindPathService {


    @Resource
    private Neo4jServiceImpl neo4jService;

    @Resource
    private EntityClassDao entityClassDao;

    @Override
    public List<Map<String, String>> getEntityType(String entityName, String endpoints) {
        String[] split = endpoints.split(",");
        String sparql = " select distinct  ?s ?type " +
                "where { " +
                " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type. " +
                " ?s <http://www.w3.org/2000/01/rdf-schema#label> ?label.  VALUES ?label {  " +
                "'" + entityName + "' '" + entityName + "'@zh '" + entityName + "'@en '" + entityName + "'@la '" + entityName + "'@ja }" +
                " } ";

        List<Map<String, String>> typeList = new ArrayList<>();
        for (String endpoint : split) {
            try {
                endpoint = endpoint.trim();
                ResultSet resultSet = RdfUtils.queryTriple(endpoint, sparql);
                List<Map<String, Object>> list = RdfUtils.resultEncapsulation(resultSet);
                if (!list.isEmpty()) {
                    for (Map<String, Object> map : list) {
                        String type = map.get("type") + "";
                        String entityIri = map.get("s") + "";
                        Boolean aBoolean = neo4jService.checkNode(type);
                        if (aBoolean) {
                            Map tmpMap = new HashMap();
                            tmpMap.put("entityIri", entityIri);
                            tmpMap.put("type", type);
                            tmpMap.put("endpoint", endpoint);
                            typeList.add(tmpMap);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return typeList;
    }


    /**
     * 从数据库中根据类名称获取类对应的uri
     * @param className 实体名称
     * @param endpoints http://micro.semweb.csdb.cn/sparql,http://chemdb.semweb.csdb.cn/sparql,http://clinicaltrials.semweb.csdb.cn/sparql,http://pubmed.semweb.csdb.cn/sparql,http://xtipc.semweb.csdb.cn/sparql,https://dbpedia.org/sparql
     * @return
     */
    @Override
    public List<Map<String, String>> getClassType(String className, String endpoints) {
        List<Map<String, String>> typeList = new ArrayList<>();
        List<EntityClass> list = new ArrayList<>();
        if (StringUtils.isNotEmpty(className)) {
            list = entityClassDao.findByLabel(className);
            for (EntityClass entityClass : list){
                if (entityClass.getEndPointsList() != null && entityClass.getEndPointsList().size() > 0){
                    boolean isExist = false;
                    for (String endPoint : entityClass.getEndPointsList()) {
                        if (endpoints.indexOf(endPoint) >=0){
                            isExist = true;
                            break;
                        }
                    }
                    if (isExist){
                        Map tmpMap = new HashMap();
                        tmpMap.put("entityIri", entityClass.getUri().trim());
                        tmpMap.put("type", entityClass.getUri().trim());
                        //tmpMap.put("endpoint", "https://www.plantplus.cn/plantsw/sparql");
                        typeList.add(tmpMap);
                    }
                }
            }
        }
        return typeList;
    }

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
    @Override
    public List<PathInfo> getPathInfo(List<Map<String, String>> xEntityTypeList, List<Map<String, String>> yEntityTypeList, List<String> endpointList, String distince, String startName, String endName) {
        List<PathInfo> list = new ArrayList<>();
        for (int i = 0; i < xEntityTypeList.size(); i++) {
            Map<String, String> xMap = xEntityTypeList.get(i);
            String xType = xMap.get("type") + "";
            String xEntityIri = xMap.get("entityIri") + "";
            for (int j = 0; j < yEntityTypeList.size(); j++) {
                Map<String, String> yMap = yEntityTypeList.get(j);
                String yType = yMap.get("type") + "";
                String yEntityIri = yMap.get("entityIri") + "";
                // 起点和终点相同不查，避免形成环
                if (!xType.equals(yType)) {
                    List<List<Relation>> path = neo4jService.findByStartAndEnd(xType, yType, distince, endpointList);
                    if (!path.isEmpty()) {
                        for (List<Relation> relations : path) {
                            PathInfo pathInfo = new PathInfo();
                            pathInfo.setPath(relations);
                            pathInfo.setStartIri(xEntityIri);
                            pathInfo.setEndIri(yEntityIri);
                            pathInfo.setStartName(startName);
                            pathInfo.setEndName(endName);
                            list.add(pathInfo);
                        }
                    }
                }
            }
        }
        return list;
    }

}


