package com.linkeddata.portal.gstore;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.linkeddata.portal.gstore.entity.Edge;
import com.linkeddata.portal.repository.daoImpl.DatasetDaoImpl;
import com.linkeddata.portal.utils.RdfUtils;
import io.swagger.annotations.Api;
import org.apache.jena.query.ResultSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : gaoshuai
 * @date : 2023/2/15 17:57
 */
@Api(tags = "gstore相关接口")
@RestController
public class QueryController {

    private static final String IP = "10.0.85.83";
    private static final Integer PORT = 9999;
    private static final String HTTP_TYPE = "ghttp";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    @Value("${sparql.endpoint}")
    private String endPoint;
    @Autowired
    private DatasetDaoImpl datasetDao;

    /**
     * 查询端点之间通路
     *
     * @return
     */
    @RequestMapping("queryRouteStr")
    public String queryRouteStr() {
        GstoreConnector gc = new GstoreConnector(IP, PORT, HTTP_TYPE, USER, PASSWORD);
        String dbName = "friend";
        String format = "json";
        String sparql = "SELECT (kHopReachablePath(<Francis>, ?x, true, -1, {<喜欢>, <关注>}) AS ?y) WHERE { <Bob> ?pred ?x . MINUS { <Francis> <不喜欢> ?x . }";
        String requestTytp = "POST";
        String query = gc.query(dbName, format, sparql, requestTytp);
        return query;
    }

    /**
     * 查询两点间通路，返回json
     *
     * @return
     */
    @RequestMapping("queryRoute")
    public JSONArray queryRoute(@RequestBody JSONObject param) {
        String dbName = (String) param.get("dbName");
        String start = (String) param.get("start");
        String end = (String) param.get("end");
        if (null == dbName || "".equals(dbName) || null == start || "".equals(start) || null == end || "".equals(end)) {
            return null;
        }
        dbName = "kualingyu";
        GstoreConnector gc = new GstoreConnector(IP, PORT, HTTP_TYPE, USER, PASSWORD);
        gc.build(dbName, "/mnt/gstore/gstore/data/kualingyu/kualingyu.nt");
        gc.load(dbName, "GET");
        String format = "json";
        String sparql = "SELECT (kHopReachablePath( <" + start + ">, ?x, false, 10, {}) AS ?y) WHERE { <" + end + "> ?pred ?x .}}";
        String requestTytp = "POST";
        String query = gc.query(dbName, format, sparql, requestTytp);
        System.out.println("sparql= " + sparql);
        System.out.println("dbName= " + dbName);
        JSONObject jsonObject = JSONObject.parseObject(query);
        String statusMsg = (String) jsonObject.get("StatusMsg");
        if ("success".equals(statusMsg)) {
            JSONObject results = (JSONObject) jsonObject.get("results");

            JSONArray bindings = (JSONArray) results.get("bindings");
            JSONObject binding = (JSONObject) bindings.get(0);
            JSONObject y = (JSONObject) binding.get("y");
            String value = (String) y.get("value");
            System.out.println("statusMsg= " + statusMsg);
            System.out.println("jsonObject= " + jsonObject);
            System.out.println("results= " + results);
            System.out.println("y= " + y);
            System.out.println("value= " + value);

            JSONObject jsonPath = JSONObject.parseObject(value);
            JSONArray paths = (JSONArray) jsonPath.get("paths");
            JSONObject path = (JSONObject) paths.get(0);
            System.out.println("paths= " + paths);
            System.out.println("findByMultipleStartAndEnd= " + path);
            return paths;
        } else {
            return null;
        }
    }


    /**
     * 查询两点间所有通路
     *
     * @param param
     * @return
     */
    @RequestMapping("queryAllRoute")
    public JSONObject queryAllRoute(@RequestBody JSONObject param) {
        String dbName = (String) param.get("dbName");
        String start = (String) param.get("start");
        String end = (String) param.get("end");
        if (null == dbName || "".equals(dbName) || null == start || "".equals(start) || null == end || "".equals(end)) {
            JSONObject js = new JSONObject();
            js.put("statusMsg", "fail");
            return js;
        }
        dbName = "kualingyu";
        GstoreConnector gc = new GstoreConnector(IP, PORT, HTTP_TYPE, USER, PASSWORD);
        gc.build(dbName, "/mnt/gstore/gstore/data/kualingyu/kualingyu.nt");
        gc.load(dbName, "GET");
        String format = "json";
        String sparql = "SELECT (kHopEnumerate( <" + start + ">, <" + end + ">, false, 10, {}) AS ?y) WHERE { }";
        String requestTytp = "POST";
        String query = gc.query(dbName, format, sparql, requestTytp);
        System.out.println("sparql= " + sparql);
        System.out.println("dbName= " + dbName);
        JSONObject jsonObject = JSONObject.parseObject(query);
        String statusMsg = (String) jsonObject.get("StatusMsg");
        if ("success".equals(statusMsg)) {
            JSONObject results = (JSONObject) jsonObject.get("results");

            JSONArray bindings = (JSONArray) results.get("bindings");
            JSONObject binding = (JSONObject) bindings.get(0);
            JSONObject y = (JSONObject) binding.get("y");
            String value = (String) y.get("value");
            System.out.println("statusMsg= " + statusMsg);
            System.out.println("jsonObject= " + jsonObject);
            System.out.println("results= " + results);
            System.out.println("y= " + y);
            System.out.println("value= " + value);

            JSONObject jsonPath = JSONObject.parseObject(value);
            JSONArray paths = (JSONArray) jsonPath.get("paths");
            if (paths.size() == 0) {
                JSONObject js = new JSONObject();
                js.put("statusMsg", "No Route");
                return js;
            }
            JSONObject path = (JSONObject) paths.get(0);
            System.out.println("paths= " + paths);
            System.out.println("findByMultipleStartAndEnd= " + path);

            // 保存nodeIndex 用于返回前端nodes去重
            Set<Integer> nodeSet = new HashSet<>();

            Set<Edge> edgeSet = new HashSet<>();

            JSONArray nodesList = new JSONArray();
            JSONArray edgesList = new JSONArray();
            for (int i = 0; i < paths.size(); i++) {
                // 遍历 jsonarray 数组，把每一个对象转成 json 对象
                JSONObject job = paths.getJSONObject(i);
                // 得到 每个对象中的属性值
                JSONArray nodes = (JSONArray) job.get("nodes");
                for (int j = 0; j < nodes.size(); j++) {
                    JSONObject json = nodes.getJSONObject(j);
                    Integer nodeIndex = (Integer) json.get("nodeIndex");
                    if (!nodeSet.contains(nodeIndex)) {
                        nodeSet.add(nodeIndex);

                        String nodeIRI = (String) json.get("nodeIRI");
                        nodeIRI = nodeIRI.replace("<", "");
                        nodeIRI = nodeIRI.replace(">", "");
                        if (start.equals(nodeIRI)) {
                            json.put("role", "start");
                        } else if (end.equals(nodeIRI)) {
                            json.put("role", "end");
                        } else {
                            json.put("role", "general");
                        }
                        nodesList.add(json);
                    }
                }
                JSONArray edges = (JSONArray) job.get("edges");
                for (int j = 0; j < edges.size(); j++) {
                    JSONObject json = edges.getJSONObject(j);
                    Edge edge = JSON.to(Edge.class, json);
                    if (!edgeSet.contains(edge)) {
                        edgeSet.add(edge);
                        edgesList.add(json);
                    }
                }
            }
            JSONObject js = new JSONObject();
            js.put("edges", edgesList);
            js.put("nodes", nodesList);
            if (edgesList.size() == 0 || nodesList.size() == 0) {
                JSONObject result = new JSONObject();
                js.put("statusMsg", "No Route");
                return result;
            } else {
                //处理edgesList，新增返回字段--label、简称
                StringBuffer nodeIRIBuf = new StringBuffer();
                //拼接iri
                for (int i = 0; i < nodesList.size(); i++){
                    JSONObject nodesListJSONObject = nodesList.getJSONObject(i);
                    String nodeIRI = nodesListJSONObject.getString("nodeIRI");
                    nodeIRIBuf.append(nodeIRI).append(",");
                }
                String nodeIRIStr = nodeIRIBuf.substring(0, nodeIRIBuf.length()-1);
                String SearchLabel = "SELECT ?g ?s ?label WHERE {" +
                        "  {" +
                        "    SERVICE SILENT <http://chemdb.semweb.csdb.cn/sparql> {" +
                        "          graph ?g {?s <http://www.w3.org/2000/01/rdf-schema#label> ?label ." +
                        "          FILTER ( ?s IN ( " + nodeIRIStr + " ) )}" +
                        "    }" +
                        "  }" +
                        "  UNION" +
                        "  {" +
                        "    SERVICE SILENT <https://www.plantplus.cn/plantsw/sparql> {" +
                        "      graph ?g {?s <http://www.w3.org/2000/01/rdf-schema#label> ?label ." +
                        "      FILTER ( ?s IN ( " + nodeIRIStr + " ) )}" +
                        "    }" +
                        "  }" +
                        "} ";
                ResultSet resultSet = RdfUtils.queryTriple(endPoint, SearchLabel);
                List<Map<String, Object>> maps = RdfUtils.resultEncapsulation(resultSet);
                //新增字段
                for (int i = 0; i < nodesList.size(); i++){
                    JSONObject nodesListJSONObject = nodesList.getJSONObject(i);
                    String nodeIRI = nodesListJSONObject.getString("nodeIRI");
                    String labelP = nodeIRI.substring(nodeIRI.lastIndexOf('/')+1,nodeIRI.length()-1);
                    String label = "";
                    String identifier = "";
                    for (Map<String, Object> map: maps
                    ) {
                        String s = "<" + map.get("s").toString() + ">";
                        if(nodeIRI.equals(s)){
                            label = map.get("label").toString();
                            identifier = datasetDao.getIdentifierByGraph(map.get("g").toString());
                            break;
                        }
                    }
                    if(label.equals("")){
                        nodesListJSONObject.put("label",labelP);
                    }else{
                        nodesListJSONObject.put("label",labelP + ":" +label);
                    }
                    nodesListJSONObject.put("identifier",identifier);
                }
                return js;
            }

        } else {
            JSONObject js = new JSONObject();
            js.put("statusMsg", "fail");
            return js;
        }
    }

  /*  public static void main(String[] args) {
        GstoreConnector gc = new GstoreConnector(IP, PORT, HTTP_TYPE, USER, PASSWORD);
        String dbName = "friend";
        String format = "json";
        String sparql = "SELECT (kHopReachablePath(<Francis>, ?x, true, -1, {<喜欢>, <关注>}) AS ?y) WHERE { <Bob> ?pred ?x . MINUS { <Francis> <不喜欢> ?x . }}";
        String requestTytp = "POST";
        String query = gc.query(dbName, format, sparql, requestTytp);

        JSONObject jsonObject = JSON.parseObject(query);
        JSONObject results = (JSONObject) jsonObject.get("results");
        String statusMsg = (String) jsonObject.get("StatusMsg");
        JSONArray bindings = (JSONArray) results.get("bindings");
        JSONObject binding = (JSONObject) bindings.get(0);
        JSONObject y = (JSONObject) binding.get("y");
        String value = (String) y.get("value");
        System.out.println(statusMsg);
        System.out.println(jsonObject);
        System.out.println(results);
        System.out.println(y);
        System.out.println(value);

        JSONObject jsonPath = JSON.parseObject(value);
        JSONArray paths = (JSONArray)jsonPath.get("paths");
        JSONObject findByMultipleStartAndEnd = (JSONObject) paths.get(0);
        System.out.println(jsonPath);
        System.out.println(findByMultipleStartAndEnd);


    }*/
/*
    public static void main(String[] args) {
        GstoreConnector gc = new GstoreConnector("127.0.0.1", 9000, "ghttp", "root", "123456");

        String dbName = "friend";
        String format = "json";
        String sparql = "SELECT (kHopReachablePath(<Francis>, ?x, true, -1, {<喜欢>, <关注>}) AS ?y) WHERE { <Bob> ?pred ?x . MINUS { <Francis> <不喜欢> ?x . }}";
        String requestTytp = "POST";
        String query = gc.query(dbName, format, sparql, requestTytp);
        System.out.println(query);
    }*/
}
