package com.linkeddata.portal.co_analysis.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.linkeddata.portal.co_analysis.repository.ResourceDao;
import com.linkeddata.portal.co_analysis.service.ResourceService;
import com.linkeddata.portal.entity.mongo.Dataset;
import com.linkeddata.portal.entity.piflowDo.PiflowDatacenter;
import com.linkeddata.portal.repository.DatasetDao;
import com.linkeddata.portal.utils.RdfUtils;
import com.linkeddata.portal.utils.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 协同分析资源相关
 *
 * @author : gaoshuai
 * @date : 2022/12/23 17:41
 */
@Service
public class ResourceServiceImpl implements ResourceService {
    /**
     * 冰川冻土dataCenterId
     */
    private static final String CRYOSPHERE = "100155";
    @Resource
    private ResourceDao resourceDao;
    @Resource
    private DatasetDao datasetDao;
    @Value("${sparql.endpoint}")
    private String endpoint;

    /**
     * 查询资源列表 (修改成从Mongodb库里相数据列表和分页,测试发现多条数据时从SPARQL里查询多条数据时速度慢)
     *            根据数据集名称模糊查询
     * @param keyword      关键词
     * @param dataCenterId piflow数据中心编号
     */
    @Override
    public String getResourceList(String keyword, String dataCenterId, Integer pageNum, Integer pageSize) {
        if (null == pageNum) {
            pageNum = 1;
        }
        if (null == pageSize) {
            pageSize = 10;
        }
        Map returnMap = new HashMap();
        returnMap.put("code", 200);
        List<Dataset> datasetList;
        String dci="";

        // 由于目前只部署好冰川冻土节点，导致接口访问异常。因此增加临时判空，改完后去掉，2023年8月23日16:41:37 陈锟
        {
            if (StringUtils.isNotBlank(dataCenterId)) {
                if (!"100106".equals(dataCenterId)) {
                    returnMap.put("code", 200);
                    returnMap.put("data", new ArrayList<>());
                    returnMap.put("total", 0);
                    returnMap.put("totalPage", 0);
                    Gson gson = new Gson();
                    String json = gson.toJson(returnMap);
                    return json;
                }
            } else {
                dataCenterId = "100106";
            }
        }

        if (null != dataCenterId && !"".equals(dataCenterId)) {
            // 查询piflow_datacenter表，通过piflow_datacenter_id查询datacenter_id
            dci = resourceDao.getDataCenterId(dataCenterId);
        }
        datasetList = datasetDao.listDatasetsPageByTitleAndDatacenterId(keyword,dci,pageNum,pageSize);
        //总条数
        Integer count = datasetDao.countDatasetsByTitleAndDatacenterId(keyword,dci);

        if (null == datasetList || datasetList.isEmpty()) {
            returnMap.put("code", 200);
            returnMap.put("data", datasetList);
            returnMap.put("total", 0);
            returnMap.put("totalPage", 0);
            Gson gson = new Gson();
            String json = gson.toJson(returnMap);
            return json;
        }

        List<Map> resultList = new ArrayList<>();

        // 遍历主语列表
        for (int i = 0; i < datasetList.size(); i++) {
            Dataset dataSet = datasetList.get(i);
            String s =  "";
            String sparqlpoint = dataSet.getSparql()+ "";

            //xiajl20230516 先从三元组文件里查询uri值
            //resultSet2 = RdfUtils.queryTriple("http://agriculture.semweb.csdb.cn/sparql?default-graph-uri=CLCD", "SELECT DISTINCT ?s WHERE { ?s a <https://schema.org/Dataset> . }");
            ResultSet resultSet = null;
            resultSet = RdfUtils.queryTriple(sparqlpoint, "SELECT DISTINCT ?s WHERE { ?s a <https://schema.org/Dataset> . }");

            String uri = "";
            while (resultSet.hasNext()) {
                uri = resultSet.nextSolution().get("s").asResource().getURI();
                s = uri;
            }

            StringBuilder sparqlStr2 = new StringBuilder();
            RdfUtils.setPreFix(sparqlStr2);

            sparqlStr2.append("SELECT  ?p ?o  ");
            sparqlStr2.append(" WHERE {  ");
//                sparqlStr2.append(" { SERVICE SILENT <").append(endpoint).append("> ");
            sparqlStr2.append(" {");
            sparqlStr2.append(" <").append(uri).append("> ?p ?o . ");
//                sparqlStr2.append(" } ");
            sparqlStr2.append(" } ");
            sparqlStr2.append(" } ");
            // 遍历mongodb中的数据集，拼接查询语句
            ResultSet resultSet2 = null;
            try {
                resultSet2 = RdfUtils.queryTriple(sparqlpoint, sparqlStr2.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            //  p : o 形式键值对
            Map<Object, Object> resourceMap = this.resultEncapsulation(resultSet2);

            String dataFeature = "";
            String name = "";
            String dataType = "";
            String description = "";
            String publisher = "";
            String stopBundle = "";

            if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#keyword")) {
                dataFeature = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#keyword") + "";
            }
            if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#name")) {
                name = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#name") + "";
            }
            if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#dataType")) {
                dataType = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#dataType") + "";
            }
            if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#description")) {
                description = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#description") + "";
            }
            if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#publisher")) {
                publisher = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#publisher") + "";
            }
            if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#stopBundle")) {
                stopBundle = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#stopBundle") + "";
            }
            // 返回的map
            Map resultMap = new HashMap();
            resultMap.put("id", s);
            String piflowDatacenterId = "";
            if (null == dataCenterId || "".equals(dataCenterId)) {
                Dataset datasetBySparql = datasetDao.getDatasetBySparql(sparqlpoint);
                String datacenterIdMongo = datasetBySparql.getDatacenterId();
                PiflowDatacenter piflowDatacenter = resourceDao.getDataCenterById(datacenterIdMongo);
                piflowDatacenterId = piflowDatacenter.getPiflowDatacenterId();
                resultMap.put("dataCenterId", piflowDatacenterId);
            } else {
                resultMap.put("dataCenterId", dataCenterId);
            }
            resultMap.put("dataFeature", dataFeature);
            resultMap.put("dataSourceName", name);
            resultMap.put("dataType", dataType);
//                resultMap.put("datasetId", "");
            resultMap.put("datasetUrl", "http://browser.semweb.csdb.cn/?" + s);
            resultMap.put("description", description);
            resultMap.put("publisher", publisher);
//                resultMap.put("selfLines", "");
//                resultMap.put("selfVolumes", "");
            resultMap.put("stopBundle", stopBundle);
            resultList.add(resultMap);
        }

        Integer totalPage = (int) Math.ceil((double) count / pageSize);
        returnMap.put("data", resultList);
        returnMap.put("total", count);
        returnMap.put("totalPage", totalPage);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(returnMap);
        return json;
    }



     /**
     * 查询资源列表
     *
     * @param keyword      关键词
     * @param dataCenterId piflow数据中心编号
     */
    public String getResourceList_Old(String keyword, String dataCenterId, Integer pageNum, Integer pageSize) {
        if (null == pageNum) {
            pageNum = 1;
        }
        if (null == pageSize) {
            pageSize = 10;
        }
        Map returnMap = new HashMap();
        returnMap.put("code", 200);
        List<Dataset> datasetList;
        if (null != dataCenterId && !"".equals(dataCenterId)) {
            // 查询piflow_datacenter表，通过piflow_datacenter_id查询datacenter_id
            String dci = resourceDao.getDataCenterId(dataCenterId);
            // 查询dataset表，通过datacenter_id查询领域内所有数据集的SPARQL地址
            datasetList = datasetDao.listDatasetsByDatacenterId(dci);
        } else {
            datasetList = datasetDao.listDatasets();
        }

        if (null == datasetList || datasetList.isEmpty()) {
            returnMap.put("code", 500);
            Gson gson = new Gson();
            String json = gson.toJson(returnMap);
            return json;
        }
        StringBuilder sparqlStr = new StringBuilder();
        RdfUtils.setPreFix(sparqlStr);
        sparqlStr.append("SELECT DISTINCT ?s  ?endpoint ");
        sparqlStr.append(" WHERE {  ");

        StringBuilder publicSparql = new StringBuilder();
        for (int i = 0; i < datasetList.size(); i++) {
            Dataset dataset = datasetList.get(i);
            if (i > 0) {
                publicSparql.append(" union \n");
            }
            publicSparql.append("{ SERVICE SILENT <").append(dataset.getSparql()).append("> \n");
            publicSparql.append("{ \n");
            publicSparql.append(" bind (\"").append(dataset.getSparql()).append("\" AS  ?endpoint ) \n");
            if (null != keyword && !"".equals(keyword.trim())) {
                keyword = StringUtil.transformSparqlStr(keyword);
                publicSparql.append(" ?s rdfs:label ?label  . \n");
                publicSparql.append(" FILTER ( contains ( ?label,\"").append(keyword).append("\") ) \n");
            }
            publicSparql.append(" ?s rdf:type <https://schema.org/Dataset>  . \n");
            publicSparql.append(" ?s <http://skos.semweb.csdb.cn/vocabulary/dataset#stopBundle> ?stopBundle . \n");
            publicSparql.append("} \n");
            publicSparql.append("} \n");
        }
        sparqlStr.append(publicSparql);
        sparqlStr.append("} ");

        StringBuilder pageStr = new StringBuilder();
        pageStr.append(" offset " + (pageNum - 1) * pageSize);
        pageStr.append(" limit " + pageSize);

        String countSparql = sparqlStr.toString();
        sparqlStr.append(pageStr);
        ResultSet resultSet = null;
        try {
            resultSet = RdfUtils.queryTriple(endpoint, sparqlStr.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ResultSet countResultSet = null;
        try {
            countResultSet = RdfUtils.queryTriple(endpoint, countSparql);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 计算总条数
        Integer count = 0;
        List<Map<String, Object>> countSubjectList = RdfUtils.resultEncapsulation(countResultSet);
        count = countSubjectList.size();
        Integer totalPage = (int) Math.ceil((double) count / pageSize);
        // 得到主语列表
        List<Map<String, Object>> subjectList = RdfUtils.resultEncapsulation(resultSet);
        List<Map> resultList = new ArrayList<>();
        if (null != subjectList && !subjectList.isEmpty()) {
            // 遍历主语列表
            for (int i = 0; i < subjectList.size(); i++) {
                Map map = subjectList.get(i);
                String s = map.get("s") + "";
                String sparqlpoint = map.get("endpoint") + "";

                StringBuilder sparqlStr2 = new StringBuilder();
                RdfUtils.setPreFix(sparqlStr2);
                sparqlStr2.append("SELECT  ?p ?o  ");
                sparqlStr2.append(" WHERE {  ");
//                sparqlStr2.append(" { SERVICE SILENT <").append(endpoint).append("> ");
                sparqlStr2.append(" {");
                sparqlStr2.append(" <").append(s).append("> ?p ?o . ");
//                sparqlStr2.append(" } ");
                sparqlStr2.append(" } ");
                sparqlStr2.append(" } ");
                // 遍历mongodb中的数据集，拼接查询语句
                ResultSet resultSet2 = null;
                try {
                    resultSet2 = RdfUtils.queryTriple(sparqlpoint, sparqlStr2.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //  p : o 形式键值对
                Map<Object, Object> resourceMap = this.resultEncapsulation(resultSet2);

                String dataFeature = "";
                String name = "";
                String dataType = "";
                String description = "";
                String publisher = "";
                String stopBundle = "";

                if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#keyword")) {
                    dataFeature = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#keyword") + "";
                }
                if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#name")) {
                    name = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#name") + "";
                }
                if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#dataType")) {
                    dataType = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#dataType") + "";
                }
                if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#description")) {
                    description = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#description") + "";
                }
                if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#publisher")) {
                    publisher = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#publisher") + "";
                }
                if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#stopBundle")) {
                    stopBundle = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#stopBundle") + "";
                }
                // 返回的map
                Map resultMap = new HashMap();
                resultMap.put("id", s);
                String piflowDatacenterId = "";
                if (null == dataCenterId || "".equals(dataCenterId)) {
                    Dataset datasetBySparql = datasetDao.getDatasetBySparql(sparqlpoint);
                    String datacenterIdMongo = datasetBySparql.getDatacenterId();
                    PiflowDatacenter piflowDatacenter = resourceDao.getDataCenterById(datacenterIdMongo);
                    piflowDatacenterId = piflowDatacenter.getPiflowDatacenterId();
                    resultMap.put("dataCenterId", piflowDatacenterId);
                } else {
                    resultMap.put("dataCenterId", dataCenterId);
                }
                resultMap.put("dataFeature", dataFeature);
                resultMap.put("dataSourceName", name);
                resultMap.put("dataType", dataType);
//                resultMap.put("datasetId", "");
                resultMap.put("datasetUrl", "http://browser.semweb.csdb.cn/?" + s);
                resultMap.put("description", description);
                resultMap.put("publisher", publisher);
//                resultMap.put("selfLines", "");
//                resultMap.put("selfVolumes", "");
                resultMap.put("stopBundle", stopBundle);
                resultList.add(resultMap);
            }
        }
        returnMap.put("data", resultList);
        returnMap.put("total", count);
        returnMap.put("totalPage", totalPage);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(returnMap);
        return json;
    }



    @Override
    public String getResourceById(String id) {
        Map resultMap = new HashMap();
        resultMap.put("code", 200);
        if (null == id || "".equals(id.trim())) {
            resultMap.put("code", 500);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(resultMap);
            return json;
        }
        int index = id.indexOf("resource");
        String url = id.substring(0, index);
        String tempStr = id.substring(7,index);
        url = url + "sparql";

        //xiajl20230517 从三元组文件里获取数据集三元组的 id
        ResultSet resultSet = null;
        resultSet = RdfUtils.queryTriple(url, "SELECT ?g WHERE { GRAPH ?g { <" + id +  "> ?p ?o . } } LIMIT 1 " ) ;
        String graphName = "";
        if (resultSet.hasNext()) {
            graphName = resultSet.nextSolution().get("g").toString().replace("http://localhost/", "");
        }
        //拼接identifier值
        //http://geoss.semweb.csdb.cn/resource/Dataset_YulinNDVI
        String idStr = tempStr.substring(0,tempStr.indexOf("semweb.csdb.cn") -1 );
        String identifier = idStr + "-" + graphName;

        List<Dataset> datasetList = resourceDao.listDatasetLikeSparql(url);
        Dataset dataset = null;
        if (null != datasetList && !datasetList.isEmpty()) {
            dataset = datasetList.get(0);
        } else {
            resultMap.put("code", 500);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(resultMap);
            return json;
        }
        PiflowDatacenter dataCenter = resourceDao.getDataCenterById(dataset.getDatacenterId());

        StringBuilder sparqlStr = new StringBuilder();
        RdfUtils.setPreFix(sparqlStr);
        sparqlStr.append("SELECT  ?p ?o ");
        sparqlStr.append(" WHERE {  ");
        sparqlStr.append(" <").append(id).append("> ?p ?o . ");
        sparqlStr.append(" }  ");

        // 遍历mongodb中的数据集，拼接查询语句
        ResultSet resultSet2 = null;
        try {
            resultSet2 = RdfUtils.queryTriple(url, sparqlStr.toString());
            if (!resultSet2.hasNext()) {
                resultMap.put("code", 500);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(resultMap);
                return json;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  p : o 形式键值对
        Map<Object, Object> resourceMap = this.resultEncapsulation(resultSet2);

        String dataFeature = "";
        String name = "";
        String dataType = "";
        String description = "";
        String publisher = "";
        String stopBundle = "";

        if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#keyword")) {
            dataFeature = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#keyword") + "";
        }
        if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#name")) {
            name = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#name") + "";
        }
        if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#dataType")) {
            dataType = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#dataType") + "";
        }
        if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#description")) {
            description = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#description") + "";
        }
        if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#publisher")) {
            publisher = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#publisher") + "";
        }
        if (null != resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#stopBundle")) {
            stopBundle = resourceMap.get("http://skos.semweb.csdb.cn/vocabulary/dataset#stopBundle") + "";
        }
        // 返回的map
        Map map = new HashMap();
        map.put("id", id);
        map.put("dataFeature", dataFeature);
        map.put("dataSourceName", name);
        map.put("dataType", dataType);
//                map.put("datasetId", "");
        map.put("datasetUrl", "http://browser.semweb.csdb.cn/?" + id);
        map.put("description", description);
        map.put("publisher", publisher);
//                map.put("selfLines", "");
//                map.put("selfVolumes", "");
        map.put("stopBundle", stopBundle);

        Map properties = new HashMap();

        //xiajl20230510 9.6项目中，hdfsPath直接从数据集字段中获取
        //根据identifier 从mongodb库里反查本数据集
        dataset = datasetDao.getDatasetByIdentifier(identifier);
        if (null != dataset){
            properties.put("hdfsPath",dataset.getHdfsPath());
        }

        properties.put("types", dataType);
//        properties.put("url", "");
        properties.put("hdfsUrl", dataCenter.getHdfs());
        map.put("properties", properties);
        resultMap.put("data", map);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(resultMap);
        return json;
    }

    /**
     * 将sparql查询的 ?p ?o 转换为 p:o 键值对
     *
     * @param resultSet
     * @return
     */
    private Map<Object, Object> resultEncapsulation(ResultSet resultSet) {
        Map<Object, Object> map = new LinkedHashMap<>();
        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.nextSolution();
            RDFNode value_p = solution.get("p");
            RDFNode value_o = solution.get("o");
            if (null != value_p) {
                map.put(value_p.isLiteral() ? value_p.asLiteral().getValue() : value_p.asResource().getURI().trim(),
                        value_o.isLiteral() ? value_o.asLiteral().getValue() : value_o.asResource().getURI().trim());
            } else {
                //增加如果该字段没有取到值存储一个空占位 前端动态展示表格
                map.put(value_p, "");
            }
        }
        return map;
    }
}
