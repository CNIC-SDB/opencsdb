package com.linkeddata.portal.script;

import com.linkeddata.portal.service.impl.Neo4jServiceImpl;
import com.linkeddata.portal.utils.RdfUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.vocabulary.RDF;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 提取端点中本体之间的关系
 *
 * @author : gaoshuai
 * @date : 2023/4/24 9:24
 */
@Api(tags = "提取端点中本体之间关系")
@Slf4j
@RestController
public class FindOntolotyRelation {
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * 默认每页10000条数据
     */
    private final Long LIMIT = 10000L;
    /**
     * 排除owl开头的实体
     */
    private final String OWL_PREFIX = "owl";
    @Resource
    private Neo4jServiceImpl neo4jService;

    /**
     * 查询端点及端点外部的本体关系
     *
     * @param endpoint 端点url
     */
    @GetMapping("/finderOntologyRelation")
    public void finderOntologyRelation(String endpoint) {

//        String endpoint = "http://xtipc.semweb.csdb.cn/sparql";
//        String endpoint = "http://pubmed.semweb.csdb.cn/sparql";
//        String endpoint = "http://clinicaltrials.semweb.csdb.cn/sparql";
//        String endpoint = "http://ncmi.semweb.csdb.cn/sparql";
//        String endpoint = "http://tpdc.semweb.csdb.cn/sparql";
//        String endpoint = "http://micro.semweb.csdb.cn/sparql"; // 时间长一直没查出来
//        this.findOntologyRelationWithOuter(endpoint);
        ArrayList<String> list = new ArrayList<>();
        list.add("http://xtipc.semweb.csdb.cn/sparql");
        list.add("http://pubmed.semweb.csdb.cn/sparql");
        list.add("http://clinicaltrials.semweb.csdb.cn/sparql");
        list.add("http://ncmi.semweb.csdb.cn/sparql");
        list.add("http://tpdc.semweb.csdb.cn/sparql");
        for (String s : list
        ) {
            this.findOntologyRelationOnlyInner(s);
        }

    }

    /**
     * 查询端点内，及端点外部的关系存入neo4j
     *
     * @param endpoint 端点url
     */
    private void findOntologyRelationWithOuter(String endpoint) {
        String fileName = endpoint.substring(endpoint.indexOf("/") + 2, endpoint.indexOf("."));
        log.info("endpoint:{}", endpoint);
        Date start = new Date();
        log.info("开始时间:{}", sdf.format(start));

        String from;
        List<String> graphList;
        // dbpedia只查这一个图
        if ("https://dbpedia.org/sparql".equals(endpoint)) {
            from = "from<http://dbpedia.org>";
        } else {
            graphList = graphFilter(endpoint);
            from = fromBuilder(graphList);
        }

        // 1、先查端点内的关系
        String relationInNode = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT DISTINCT ?sType ?p  ?oType   " +
                from +
                " WHERE { " +
                "    ?s rdf:type ?sType .  ?s ?p ?o .   ?o  rdf:type ?oType. " +
                " filter(isIri(?o)) . filter(?p !=<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>) . FILTER regex(str(?oType), \"^http\"). " +
                " } ";
        try {
            ResultSet resultSet1 = RdfUtils.queryTriple(endpoint, relationInNode + this.offsetAndLimit(0L, LIMIT));
            List<Map<String, Object>> relationInNodeList = RdfUtils.resultEncapsulation(resultSet1);
            Integer relationInNodeOffset = 1;
            if (!relationInNodeList.isEmpty()) {
                for (Map map : relationInNodeList) {
                    String sType = map.get("sType") + "";
                    String oType = map.get("oType") + "";
                    String p = map.get("p") + "";
                    addToNeo4j(endpoint, sType, oType, p);
                }
            }
            // 处理分页
            while (!relationInNodeList.isEmpty()) {
                relationInNodeOffset++;
                ResultSet resultSet1_1 = RdfUtils.queryTriple(endpoint, relationInNode + this.offsetAndLimit((relationInNodeOffset * LIMIT), LIMIT));
                List<Map<String, Object>> relationInNodeListTmp = RdfUtils.resultEncapsulation(resultSet1_1);
                if (!relationInNodeListTmp.isEmpty()) {
                    for (Map map : relationInNodeListTmp) {
                        String sType = map.get("sType") + "";
                        String oType = map.get("oType") + "";
                        String p = map.get("p") + "";
                        addToNeo4j(endpoint, sType, oType, p);
                    }
                }
                relationInNodeList.clear();
                relationInNodeList.addAll(relationInNodeListTmp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 2、再查不再端点内的关系
        String relationNotInNode = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "SELECT DISTINCT ?sType ?p  ?oType   " +
                from +
                " WHERE { " +
                "   ?s rdf:type ?sType .  ?s ?p ?o .    optional{?o rdf:type ?oType.}  " +
                " filter(isIri(?o)) . FILTER  (!BOUND(?oType)).  filter(?p !=<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>) ." +
                " } ";
        try {
            ResultSet resultSet2 = RdfUtils.queryTriple(endpoint, relationNotInNode + this.offsetAndLimit(0L, LIMIT));
            List<Map<String, Object>> relationNotInNodeList = RdfUtils.resultEncapsulation(resultSet2);
            for (Map map : relationNotInNodeList) {
                String sType = map.get("sType") + "";
                String p = map.get("p") + "";
                // 2.1、根据查出的主语 谓语 去查，这些主语+谓语在这个端点内的所有宾语，然后遍历解析宾语
                String findObject = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                        "construct { ?s ?p  ?o }   " +
                        from +
                        " WHERE { " +
                        "    ?s ?p ?o .  ?s a ?sType.  filter(isIri(?o)) . " +
                        " filter(?sType=<" + sType + "> && ?p=<" + p + ">).  filter(?p !=<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>) . FILTER regex(str(?o), \"^http\")  " +
                        " } ";
                try {
                    RDFConnection conn = RDFConnectionRemote.service(endpoint).build();
                    Model findObjectModel = conn.queryConstruct(findObject + this.offsetAndLimit(0L, LIMIT));
                    NodeIterator nodeIterator = findObjectModel.listObjects();
                    if (nodeIterator.hasNext()) {
                        RDFNode next = nodeIterator.next();
                        List<String> objectTypeList = resolveIriByModel(next.asResource().getURI());
                        for (String objectType : objectTypeList) {
                            addToNeo4j(endpoint, sType, objectType, p);
                        }
                    }
                    // 处理 findObject 查出的主语 谓语 ?object 的分页
                    int findObjectOffset = 0;
                    while (!findObjectModel.isEmpty()) {
                        findObjectOffset++;
                        RDFConnection conn1 = RDFConnectionRemote.service(endpoint).build();
                        Model findObjectModel1 = conn1.queryConstruct(findObject + this.offsetAndLimit(findObjectOffset * LIMIT, LIMIT));
                        NodeIterator nodeIterator1 = findObjectModel1.listObjects();
                        if (nodeIterator1.hasNext()) {
                            RDFNode next1 = nodeIterator1.next();
                            List<String> objectTypeList = resolveIriByModel(next1.asResource().getURI());
                            for (String objectType : objectTypeList) {
                                addToNeo4j(endpoint, sType, objectType, p);
                            }
                        }
                        findObjectModel.removeAll();
                        findObjectModel.add(findObjectModel1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 处理 不再端点内的关系relationNotInNode 的分页
            int relationNotInNodeOffset = 0;
            while (!relationNotInNodeList.isEmpty()) {
                relationNotInNodeOffset++;
                ResultSet resultSet1 = RdfUtils.queryTriple(endpoint, relationNotInNode + this.offsetAndLimit((relationNotInNodeOffset * LIMIT), LIMIT));
                List<Map<String, Object>> list1 = RdfUtils.resultEncapsulation(resultSet1);
                for (Map map : list1) {
                    String sType = map.get("sType") + "";
                    String p = map.get("p") + "";
                    // 2.1、根据查出的主语 谓语 去查，这些主语+谓语在这个端点内的所有宾语，然后遍历解析宾语
                    String findObject = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                            "construct { ?s ?p  ?o }   " +
                            from +
                            " WHERE { " +
                            "    ?s ?p ?o .  ?s a ?sType.  filter(isIri(?o)) . " +
                            " filter(?sType=<" + sType + "> && ?p=<" + p + ">).  filter(?p !=<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>) . FILTER regex(str(?o), \"^http\")  " +
                            " } ";
                    try {
                        RDFConnection conn2 = RDFConnectionRemote.service(endpoint).build();
                        Model model = conn2.queryConstruct(findObject + this.offsetAndLimit(0L, LIMIT));
                        NodeIterator nodeIterator = model.listObjects();
                        if (nodeIterator.hasNext()) {
                            RDFNode next = nodeIterator.next();
                            List<String> objectTypeList = resolveIriByModel(next.asResource().getURI());
                            for (String objectType : objectTypeList) {
                                addToNeo4j(endpoint, sType, objectType, p);
                            }
                        }
                        int findObjectOffset2 = 0;
                        while (!model.isEmpty()) {
                            findObjectOffset2++;
                            RDFConnection conn1 = RDFConnectionRemote.service(endpoint).build();
                            Model model1 = conn1.queryConstruct(findObject + this.offsetAndLimit((findObjectOffset2 * LIMIT), LIMIT));
                            NodeIterator nodeIterator1 = model1.listObjects();
                            if (nodeIterator1.hasNext()) {
                                RDFNode next1 = nodeIterator1.next();
                                List<String> objectTypeList = resolveIriByModel(next1.asResource().getURI());
                                for (String objectType : objectTypeList) {
                                    addToNeo4j(endpoint, sType, objectType, p);
                                }
                            }
                            model.removeAll();
                            model.add(model1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                relationNotInNodeList.clear();
                relationNotInNodeList.addAll(list1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Date end = new Date();
        log.info("结束时间:{}", sdf.format(end));
    }


    @GetMapping(value = "/12")
    public void te() {
        findOntologyRelationOnlyInner("http://dbpedia.org/sparql");
    }

    /**
     * 只查询端点内部的关系，存入neo4j
     * 植物、化合物、微生物只查内部关系
     *
     * @param endpoint
     */
    public void findOntologyRelationOnlyInner(String endpoint) {
        String fileName = endpoint.substring(endpoint.indexOf("/") + 2, endpoint.indexOf("."));
        log.info("endpoint:{}", endpoint);
        Date start = new Date();
        log.info("开始时间:{}", sdf.format(start));

        String from;
        List<String> graphList;
        // dbpedia只查这一个图
        /*if ("https://dbpedia.org/sparql".equals(endpoint)) {
            from = "from<http://dbpedia.org>";
        } else {
            graphList = graphFilter(endpoint);
            from = fromBuilder(graphList);
        }*/

        // 1、先查端点内的关系
//        String relationInNode = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
//                "SELECT DISTINCT ?sType ?p  ?oType   " +
//                from +
//                " WHERE { " +
//                "    ?s rdf:type ?sType .  ?s ?p ?o .   ?o  rdf:type ?oType. " +
//                " filter(isIri(?o)) . filter(?p !=<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>) . FILTER regex(str(?oType), \"^http\"). " +
//                " } ";

        String relationInNode = """
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                SELECT ?sType ?p ?oType
                FROM <http://dbpedia.org>
                WHERE {
                  ?p rdfs:domain ?sType .
                  ?p rdfs:range ?oType .
                  ?oType a owl:Class .
                }
                """;
        try {
            ResultSet resultSet1 = RdfUtils.queryTriple(endpoint, relationInNode + this.offsetAndLimit(0L, LIMIT));
            List<Map<String, Object>> relationInNodeList = RdfUtils.resultEncapsulation(resultSet1);
            Integer relationInNodeOffset = 1;
            if (!relationInNodeList.isEmpty()) {
                for (Map map : relationInNodeList) {
                    String sType = map.get("sType") + "";
                    String oType = map.get("oType") + "";
                    String p = map.get("p") + "";
                    addToNeo4j(endpoint, sType, oType, p);
                }
            }
            // 处理分页
            while (!relationInNodeList.isEmpty()) {
                relationInNodeOffset++;
                ResultSet resultSet1_1 = RdfUtils.queryTriple(endpoint, relationInNode + this.offsetAndLimit((relationInNodeOffset * LIMIT), LIMIT));
                List<Map<String, Object>> relationInNodeListTmp = RdfUtils.resultEncapsulation(resultSet1_1);
                if (!relationInNodeListTmp.isEmpty()) {
                    for (Map map : relationInNodeListTmp) {
                        String sType = map.get("sType") + "";
                        String oType = map.get("oType") + "";
                        String p = map.get("p") + "";
                        addToNeo4j(endpoint, sType, oType, p);
                    }
                }
                relationInNodeList.clear();
                relationInNodeList.addAll(relationInNodeListTmp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Date end = new Date();
        log.info("结束时间:{}", sdf.format(end));
    }

    /**
     * 调用保存至neo4j的方法，将数据保存至neo4j
     *
     * @param endpoint 所属端点
     * @param sType    主语
     * @param oType    宾语
     * @param p        谓语
     */
    private void addToNeo4j(String endpoint, String sType, String oType, String p) {
        if (!sType.equals(oType)) {
            String sTypeSuffix = RdfUtils.getIriSuffix(sType);
            String oTypeSuffix = RdfUtils.getIriSuffix(oType);
            if (!(OWL_PREFIX.equals(sTypeSuffix) || OWL_PREFIX.equals(oTypeSuffix))) {
                List<String> list1 = new ArrayList<>();
                list1.add(sType);
                list1.add(p);
                list1.add(oType);
                list1.add(endpoint);
                List list2 = new ArrayList<>();
                list2.add(list1);
                neo4jService.savePoint(list2);
            }
        }
    }

    /**
     * 使用rdf浏览器的解析方式解析iri
     *
     * @param iri
     * @return
     */
    private List<String> resolveIriByModel(String iri) {
        if (iri.contains("dbpedia.org/resource")) {
            iri = iri.replaceAll("dbpedia.org/resource", "dbpedia.org/data") + ".ttl";
        }
        Model model = ModelFactory.createDefaultModel();
        try {
            String finalIri = iri;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // 要执行的代码
                    model.read(finalIri);
                }
            });
            thread.start();
            thread.join(2000); // 设置超时时间为 1 秒

            if (thread.isAlive()) {
                thread.interrupt(); // 如果线程还在运行，则中断该线程
            }

        } catch (Exception e) {
            log.info(iri);
            e.printStackTrace();
        }
        NodeIterator nodeIterator = model.listObjectsOfProperty(RDF.type);
        List<String> list = new ArrayList<>();
        while (nodeIterator.hasNext()) {
            RDFNode next = nodeIterator.next();
            String uri = next.asResource().getURI();
            if (uri.startsWith("http")) {
                list.add(uri);
            }
        }
        return list;
    }

    /**
     * 过滤掉端点中virtuoso默认的图
     *
     * @param endpoint
     * @return
     */
    private List<String> graphFilter(String endpoint) {
        List<String> graphList = new ArrayList<>();
        String sparql = "SELECT DISTINCT ?g \n" +
                "WHERE { \n" +
                "  GRAPH ?g { \n" +
                "    ?s ?p ?o . \n" +
                "    FILTER (!regex(str(?g), \"^http://www.openlinksw.com/schemas/.* \"))\n" +
                "  } \n" +
                "} limit 10  ";
        List<Map<String, Object>> list = null;
        try {
            ResultSet resultSet = RdfUtils.queryTriple(endpoint, sparql);
            list = RdfUtils.resultEncapsulation(resultSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> excludeGraphList = new ArrayList<>();
        excludeGraphList.add("http://www.w3.org/2002/07/owl#");
        excludeGraphList.add("urn:core:services:sparql");
        excludeGraphList.add("http://www.w3.org/ns/ldp#");
        excludeGraphList.add("urn:activitystreams-owl:map");
        excludeGraphList.add("http://localhost:8890/DAV/");
        excludeGraphList.add("http://www.openlinksw.com/schemas/virtrdf#");
        for (Map map : list) {
            String graph = map.get("g") + "";
            if (!excludeGraphList.contains(graph)) {
                graphList.add(graph);
            }
        }
        return graphList;
    }

    /**
     * 生成从哪个图里查数据
     *
     * @param graphList
     * @return
     */
    private String fromBuilder(List<String> graphList) {
        StringBuilder sb = new StringBuilder();
        if (graphList != null && !graphList.isEmpty()) {
            for (String graph : graphList) {
                sb.append(" from <").append(graph).append("> ");
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * 拼接offset 和limit
     *
     * @param pageSize
     * @param limit
     * @return
     */
    private String offsetAndLimit(Long pageSize, Long limit) {
        return " offset " + pageSize + " LIMIT " + limit + " ";
    }


}
