package com.linkeddata.portal.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.linkeddata.portal.entity.script.findlink.ClinicalStudyEntity;
import com.linkeddata.portal.entity.script.findlink.NcmiClinicalDrug;
import com.linkeddata.portal.entity.script.findlink.NcmiClinicalTrail;
import com.linkeddata.portal.service.FindLinksService;
import com.linkeddata.portal.service.helper.FindLinksHelper;
import com.linkeddata.portal.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtModel;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.jena.datatypes.xsd.XSDDatatype.XSDanyURI;

/**
 * 实现类
 *
 * @author wangzhiliang
 */
@Service
@Slf4j
public class FindLinksServiceImpl implements FindLinksService {
    @Value("${virtuoso.addr}")
    public String virtuosoIp;
    @Value("${virtuoso.user}")
    public String virtuosoUser;
    @Value("${virtuoso.password}")
    public String virtuosoPassword;
    private static final String TEMP_GRAPH = "http://localhost/GeoTemp";

    @Override
    public void getPlantLinksByDbpedia() {
        Long start = System.currentTimeMillis();
        //清空临时存储的数据库图
        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
        graphVirt.clear();
        try {
            //查询植物物种端点
            int k = 1;
            String plantString = "select * where { \n" +
                    "\t?s  <http://rs.tdwg.org/dwc/terms/genus> ?o \n" +
                    "} \n";
            String countLocal = "select  (count(*) as ?count ) where { \n" + plantString + " \n}";
            ResultSet countResultSet = RdfUtils.queryTriple("http://10.0.82.94:8890/sparql?default-graph-uri=http://localhost/Taxon", countLocal);
            int localCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                localCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            log.info("符合关联数据总条数 ：{}", localCount);
            for (int offset1 = 0, limit1 = 1000; offset1 < localCount; offset1 += limit1) {
                List<Statement> statementList = new ArrayList<>();
                ResultSet resultSet = RdfUtils.queryTriple("http://10.0.82.94:8890/sparql?default-graph-uri=http://localhost/Taxon", plantString + " offset " + offset1 + " limit " + limit1);
                while (resultSet.hasNext()) {
                    QuerySolution plantSolution = resultSet.nextSolution();
                    Resource plantSub = plantSolution.getResource("s");
                    RDFNode rdfNode = plantSolution.get("o");
                    //查询dbpedia 关联关系
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n");
                    stringBuilder.append("select * where { \n");
                    stringBuilder.append("\t?s rdfs:label '").append(rdfNode.asLiteral().getValue()).append("'@en \n");
                    stringBuilder.append("}");
                    ResultSet dbpediaResultSet = RdfUtils.queryTriple("https://dbpedia.org/sparql", stringBuilder.toString());
                    while (dbpediaResultSet.hasNext()) {
                        QuerySolution dbpediaSolution = dbpediaResultSet.nextSolution();
                        Resource dbpSub = dbpediaSolution.getResource("s");
                        //生成新的关系
                        statementList.add(new StatementImpl(new ResourceImpl(plantSub.getURI()), RDFS.subClassOf, new ResourceImpl(dbpSub.getURI())));
                    }

                }
                VirtModel model = new VirtModel(graphVirt);
                log.info("存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statementList.size());
                model.add(statementList);
                log.info("已经执行完成 {} 次", k++);
            }
            //写入文件
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            String filePath = "/mnt/wangzl/taxonLinkDbpedia.ttl";
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }
    }

    @Override
    public void getMircoProteinLinksProteinOntology() {
        Long start = System.currentTimeMillis();
        //清空临时存储的数据库图
        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
//        graphVirt.clear();
        try {
            int k = 1;
            //Protein  蛋白质所有的 name 和 class 值
            String proteinQuery = " select * where { \n" +
//        proteinQuery.append(" BIND (<http://micro.semweb.csdb.cn/resource/Protein/QNO95585.1>  as ?s).\n");
                    " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://nmdc.cn/ontology/ncov/Protein> .\n" +
                    " ?s <http://nmdc.cn/ontology/ncov/name> ?name .\n" +
                    " ?s <http://nmdc.cn/ontology/ncov/class> ?class .\n" +
                    " }\n";
            String countLocal = "select  (count(*) as ?count ) where { \n" + proteinQuery + " \n}";
            ResultSet countResultSet = RdfUtils.queryTriple("http://10.0.82.94:8890/sparql?default-graph-uri=Protein", countLocal);
            int localCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                localCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            log.info("符合关联数据总条数 ：{}", localCount);
            for (int offset1 = 0, limit1 = 10000; offset1 < localCount; offset1 += limit1) {
                List<Statement> statementList = new ArrayList<>();
                ResultSet resultSet = RdfUtils.queryTriple("http://10.0.82.94:8890/sparql?default-graph-uri=Protein", proteinQuery + " offset " + offset1 + " limit " + limit1);
                if(null != resultSet){
                    while (resultSet.hasNext()) {
                        QuerySolution plantSolution = resultSet.nextSolution();
                        Resource proteinSub = plantSolution.getResource("s");
                        RDFNode name = plantSolution.get("name");
                        RDFNode proClass = plantSolution.get("class");
                        StringBuilder ontology = new StringBuilder();
                        ontology.append("prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n");
                        ontology.append("select  * where { \n");
                        ontology.append("\t?s rdfs:label ?o   \n");
                        ontology.append("\tfilter (strStarts(str(?o),'").append(StringUtil.transformSparqlStr(name.asLiteral().getValue().toString())).append(" ') && contains(str(?o),'").append(StringUtil.transformSparqlStr(proClass.asLiteral().getValue().toString())).append("') )  \n");
                        ontology.append("\t}\n");
                        //增加 该蛋白质主语 owl:sameAs ProteinOntology 主语 关系
                        ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.89.33:8890/sparql?default-graph-uri=http://localhost/ProteinOntology", ontology.toString());
                        if(null !=ontologyResult){
                            while (ontologyResult.hasNext()) {
                                QuerySolution solution = ontologyResult.nextSolution();
                                Resource ontologySub = solution.getResource("s");
                                //生成新的关系
                                statementList.add(new StatementImpl(new ResourceImpl(proteinSub.getURI()), OWL.sameAs, new ResourceImpl(ontologySub.getURI())));
                            }
                        }
                    }
                }
                VirtModel model = new VirtModel(graphVirt);
                log.info("存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statementList.size());
                model.add(statementList);
                log.info("已经执行完成 {} 次", k++);
            }
            //写入文件
            ResultSet ontologyResult = RdfUtils.queryTriple("http://" + virtuosoIp + ":8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            String filePath = "/mnt/wangzl/proteinLinkDbpedia.ttl";
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }

    }

    @Override
    public void linkedCommon() {
        Long start = System.currentTimeMillis();
        //是否拼接语种
        boolean lang = false;
        //动态字段
        String dynamicFiled = "o";
        //进行关联的端点带图
        String localEndPoint = "http://10.0.82.94:8890/sparql?default-graph-uri=http://localhost/Taxon";
        //进行关联的数据查询语句
        String localSparql = "select * where { ?s   <http://www.w3.org/2000/01/rdf-schema#label>  ?o   } ";
        //关联到的端点
        String linkEndPoint = "http://graph.openbiodiv.net/repositories/OpenBiodiv";
        //生成文件路径
        String filePath = "/mnt/wangzl/linkBio.ttl";
        //清空临时存储的数据库图
        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
        graphVirt.clear();
        try {
            int k = 0;
            String countLocal = "select  (count(*) as ?count ) where { \n" + localSparql + " \n}";
            ResultSet countResultSet = RdfUtils.queryTriple(localEndPoint, countLocal);
            int localCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                localCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            log.info("符合关联数据总条数 ：{}", localCount);
            for (int offset1 = 0, limit1 = 10000; offset1 < localCount; offset1 += limit1) {
                //查询本地需要动态获取的数据
                ResultSet resultSet = RdfUtils.queryTriple(localEndPoint, localSparql + " offset " + offset1 + " limit " + limit1);
                List<Map<String, Object>> localResults = RdfUtils.resultEncapsulation(resultSet);
                log.info("符合关联数据 : {} 条  ", localResults.size());
                List<String> rvs = resultSet.getResultVars();
                String subject = rvs.get(0);
                //每一千条数据作为一块进行处理
                for (int offset = 0, limit = 1000; offset < localResults.size(); offset += limit) {
                    List<Map<String, Object>> localResultSub = localResults.subList(offset, Math.min(offset + limit, localResults.size()));
                    List<Statement> statementList = new ArrayList<>();
                    for (int i = 0; i < localResultSub.size(); i++) {
                        Map<String, Object> tempMap = localResultSub.get(i);
                        String sub = String.valueOf(tempMap.get(subject));
                        String dyf = String.valueOf(tempMap.get(dynamicFiled));
                        String languaue = String.valueOf(tempMap.get("lang"));
                        String contact;
                        if (lang) {
                            contact = "'" + StringUtil.transformSparqlStr(dyf) + "'@" + languaue;
                        } else {
                            contact = "'" + StringUtil.transformSparqlStr(dyf) + "'";
                        }
                        String sparql = " select * where {  ?s  <http://www.w3.org/2000/01/rdf-schema#label>  " + contact.trim() + " . }";
                        ResultSet lingResult = RdfUtils.queryTriple(linkEndPoint, sparql);
                        if (null != lingResult) {
                            while (lingResult.hasNext()) {
                                String linkSubj = lingResult.getResultVars().get(0);
                                QuerySolution solution = lingResult.nextSolution();
                                Resource linkSub = solution.getResource(linkSubj);
                                statementList.add(new StatementImpl(new ResourceImpl(sub), OWL.sameAs, new ResourceImpl(linkSub.getURI())));
                            }
                        }
                    }
                    VirtModel model = new VirtModel(graphVirt);
                    log.info("存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statementList.size());
                    model.add(statementList);
                    log.info("已经执行完成 {} 次", k++);
                }
            }
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
//            //执行结束后清空图
//            graphVirt.clear();
            model.close();
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }

    }

    @Override
    public void linkedLabelProperty(String localEndPoint, String linkEndPoint, String filePath) {
        // 进行关联的端点带图
        localEndPoint = "http://10.0.90.212:8899/sparql";
        // 关联到的端点
        linkEndPoint = "https://dbpedia.org/sparql";

        Long start = System.currentTimeMillis();
        // 是否拼接语种
        boolean lang = false;
        // 动态字段
        String dynamicFiled = "o";
        // 进行关联的数据查询语句，查本地发布多少数据 from <CoLChina_sp2000>
        String localSparql = "select * where { ?s   <http://www.w3.org/2000/01/rdf-schema#label>  ?o   } ";
        // 生成文件路径
//        filePath = "/mnt/wangzl/linkBio.ttl";
//        String   filePath = "E:\\mnt\\linkBio.ttl";
        filePath = "/mnt/gaos/linkBio.ttl";
        // 连接图数据库
        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://10.0.89.33:1111", "dba", "0dabigta1357");
        // 清空临时存储的数据库图
        graphVirt.clear();
        try {
            int k = 0;
            // virtuoso直接查询，最多显示10000条记录，需要分页遍历
            String countLocal = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "select  (count(*) as ?count )    \n" +
                    "where {\n" +
                    "  graph <CoLChina_sp2000> {\n" +
                    "    ?s  <http://www.w3.org/2000/01/rdf-schema#label>  ?o  \n" +
                    "  }   \n" +
                    "}";
            ResultSet countResultSet = RdfUtils.queryTriple(localEndPoint, countLocal);
//            ResultSet countResultSet = RdfUtils.sparqlSelect(countLocal);
            int localCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                localCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            log.info("符合关联数据总条数 ：{}", localCount);
            int yichuli = 0;
            for (int offset1 = 0, limit1 = 10000; offset1 < localCount; offset1 += limit1) {
                //查询本地需要动态获取的数据
                ResultSet resultSet = RdfUtils.queryTriple(localEndPoint, localSparql + " offset " + offset1 + " limit " + limit1);
                List<Map<String, Object>> localResults = RdfUtils.resultEncapsulation(resultSet);
                log.info("符合关联数据 : {} 条  ", localResults.size());
                List<String> rvs = resultSet.getResultVars();
                String subject = rvs.get(0);
                //每一千条数据作为一块进行处理
                for (int offset = 0, limit = 1000; offset < localResults.size(); offset += limit) {
                    List<Map<String, Object>> localResultSub = localResults.subList(offset, Math.min(offset + limit, localResults.size()));
                    List<Statement> statementList = new ArrayList<>();
                    for (int i = 0; i < localResultSub.size(); i++) {
                        yichuli++;
                        log.info("已处理{}条", yichuli);
                        Map<String, Object> tempMap = localResultSub.get(i);
                        String sub = String.valueOf(tempMap.get(subject));
                        String dyf = String.valueOf(tempMap.get(dynamicFiled));
                        String languaue = String.valueOf(tempMap.get("lang"));
                        String contact;
                        if (lang) {
                            contact = "'" + StringUtil.transformSparqlStr(dyf) + "'@" + languaue;
                        } else {
                            contact = "'" + StringUtil.transformSparqlStr(dyf) + "'";
                        }
                        contact = contact.trim();
//                        String sparql = " select * where {  ?s  <http://www.w3.org/2000/01/rdf-schema#label>  " + contact.trim() + " . }";
                      /*  String sparql = " select * where {  ?s  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . VALUES ?label { " +
                                "'" + contact + "' '" + contact + "'@zh '" + contact + "'@en '" + contact + "'@la '" + contact + "'@ja }" +
                                "}";*/
                        String sparql = " select * where {  ?s  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . VALUES ?label { " +
                                contact + " " + contact + "@zh " + contact + "@en " + contact + "@la " + contact + "@ja }" +
                                "}";
                        ResultSet lingResult = RdfUtils.queryTriple(linkEndPoint, sparql);
                        if (null != lingResult) {
                            while (lingResult.hasNext()) {
                                String linkSubj = lingResult.getResultVars().get(0);
                                QuerySolution solution = lingResult.nextSolution();
                                Resource linkSub = solution.getResource(linkSubj);
                                statementList.add(new StatementImpl(new ResourceImpl(sub), OWL.sameAs, new ResourceImpl(linkSub.getURI())));
                            }
                        }
                    }
                    VirtModel model = new VirtModel(graphVirt);
                    log.info("存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statementList.size());
                    model.add(statementList);
                    log.info("已经执行完成 {} 次", k++);
                }
            }
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.89.33:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
//            //执行结束后清空图
//            graphVirt.clear();
            model.close();
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }
    }

    @Override
    public void linkedGeoNames(String localEndPoint, String linkEndPoint, String filePath) {
        // 进行关联的端点带图
        localEndPoint = "http://10.0.90.212:8899/sparql";
        // 关联到的端点
        linkEndPoint = "http://linkedgeodata.org/sparql";
        // 创建与geoName关联的谓语
        Model tmpModel = ModelFactory.createDefaultModel();
        Property distribution = tmpModel.createProperty("http://schema.org/distribution");

        Long start = System.currentTimeMillis();
        // 是否拼接语种
        boolean lang = false;
        // 动态字段
        String dynamicFiled = "o";
        // 进行关联的数据查询语句，查本地发布多少数据 from <CoLChina_sp2000>
//        String localSparql = "select * where { ?s   <http://schema.org/distribution>  ?o   } ";
//        String localSparql = "select * where {  ?s  <http://schema.org/distribution>  ?o . filter (?s = <http://ioz.semweb.csdb.cn/resource/Species_Clitocybe_clavipes>)  } ";
        String localSparql = "select * where {  ?s  <http://schema.org/distribution>  ?o .   } ";
        // 生成文件路径
//        filePath = "/mnt/wangzl/linkBio.ttl";
//        String   filePath = "E:\\mnt\\linkBio.ttl";
        filePath = "/mnt/gaos/linkGeo.ttl";
        // 连接图数据库
        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://10.0.89.33:1111", "dba", "0dabigta1357");
        // 清空临时存储的数据库图
        graphVirt.clear();
        try {
            int k = 0;
            // virtuoso直接查询，最多显示10000条记录，需要分页遍历
            String countLocal = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "select  (count(*) as ?count )    \n" +
                    "where {\n" +
                    "  graph <CoLChina_sp2000> {\n" +
                    "    ?s  <http://schema.org/distribution>  ?o  \n" +
                    "  }   \n" +
                    "}";
            ResultSet countResultSet = RdfUtils.queryTriple(localEndPoint, countLocal);
            int localCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                localCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            log.info("符合关联数据总条数 ：{}", localCount);
            int yichuli = 0;
            for (int offset1 = 0, limit1 = 10000; offset1 < localCount; offset1 += limit1) {
                //查询本地需要动态获取的数据
                ResultSet resultSet = RdfUtils.queryTriple(localEndPoint, localSparql + " offset " + offset1 + " limit " + limit1);
                List<Map<String, Object>> localResults = RdfUtils.resultEncapsulation(resultSet);
                log.info("符合关联数据 : {} 条  ", localResults.size());
                List<String> rvs = resultSet.getResultVars();
                String subject = rvs.get(0);
                //每一千条数据作为一块进行处理
                for (int offset = 0, limit = 1000; offset < localResults.size(); offset += limit) {
                    List<Map<String, Object>> localResultSub = localResults.subList(offset, Math.min(offset + limit, localResults.size()));
                    List<Statement> statementList = new ArrayList<>();
                    for (int i = 0; i < localResultSub.size(); i++) {
                        yichuli++;
                        log.info("已处理{}条", yichuli);
                        Map<String, Object> tempMap = localResultSub.get(i);
                        String sub = String.valueOf(tempMap.get(subject));
                        String dyf = String.valueOf(tempMap.get(dynamicFiled));
                        String languaue = String.valueOf(tempMap.get("lang"));
                        String[] dyfs = dyf.split(",");
                        for (String con : dyfs) {
                            String contact;
                            if (lang) {
                                contact = "'" + StringUtil.transformSparqlStr(con) + "'@" + languaue;
                            } else {
                                contact = "'" + StringUtil.transformSparqlStr(con) + "'";
                            }
                            contact = contact.trim();
                            String sparql = " select * from <http://linkedgeodata.org/geonames> " +
                                    " where {   ?s  ?p  ?label  . " +
                                    " optional { ?s  ?p  ?label  . } " +
                                    " filter (?p =  <http://www.geonames.org/ontology#alternateName> ) " +
                                    " VALUES ?label { " +
                                    contact + " " + contact + "@zh " + contact + "@en " + contact + "@la " + contact + "@ja }" +
                                    " } " +
                                    " limit 1 ";
                            ResultSet lingResult = RdfUtils.queryTriple(linkEndPoint, sparql);
                            if (null != lingResult) {
                                while (lingResult.hasNext()) {
                                    String linkSubj = lingResult.getResultVars().get(0);
                                    QuerySolution solution = lingResult.nextSolution();
                                    Resource linkSub = solution.getResource(linkSubj);
                                    statementList.add(new StatementImpl(new ResourceImpl(sub), distribution, new ResourceImpl(linkSub.getURI())));
                                }
                            }
                        }
                    }
                    VirtModel model = new VirtModel(graphVirt);
                    log.info("存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statementList.size());
                    model.add(statementList);
                    log.info("已经执行完成 {} 次", k++);
                }
            }
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.89.33:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
//            //执行结束后清空图
//            graphVirt.clear();
            model.close();
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }
    }

    //    @Override
//    public void linkedLabelProperty(String localEndPoint, String linkEndPoint, String filePath) {
//        // 进行关联的端点带图
////        localEndPoint = "http://10.0.89.33:8890/sparql?default-graph-uri=http://localhost:8890/animal";
////        localEndPoint = "http://10.0.90.212:8899/sparql?default-graph-uri=http://localhost:8890/animal_test";
//        localEndPoint = "http://10.0.90.212:8899/sparql";
//        // 关联到的端点
//        linkEndPoint = "https://dbpedia.org/sparql";
//
//        Long start = System.currentTimeMillis();
//        // 是否拼接语种
//        boolean lang = false;
//        // 动态字段
//        String dynamicFiled = "o";
//        // 进行关联的数据查询语句，查本地发布多少数据 from <CoLChina_sp2000>
//        String localSparql = "select * where { ?s   <http://www.w3.org/2000/01/rdf-schema#label>  ?o   } ";
//        // 生成文件路径
////        filePath = "/mnt/wangzl/linkBio.ttl";
//        filePath = "E:\\mnt\\linkBio.ttl";
//        // 连接图数据库
//        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
//        // 清空临时存储的数据库图
//        graphVirt.clear();
//        try {
//            int k = 0;
//            // virtuoso直接查询，最多显示10000条记录，需要分页遍历
////            String countLocal = "select  (count(*) as ?count ) where { \n" + localSparql + " \n}";
////            String countLocal = "select  (count(*) as ?count )    where { SERVICE SILENT <http://10.0.90.212:8899/sparql> {  " + localSparql + " } }";
//            String countLocal = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
//                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
//                    "select  (count(*) as ?count )    \n" +
//                    "where {\n" +
//                    "  graph <CoLChina_sp2000> {\n" +
//                    "    ?s  <http://www.w3.org/2000/01/rdf-schema#label>  ?o  \n" +
//                    "  }   \n" +
//                    "}";
//            ResultSet countResultSet = RdfUtils.queryTriple(localEndPoint, countLocal);
////            ResultSet countResultSet = RdfUtils.sparqlSelect(countLocal);
//            int localCount = 0;
//            while (countResultSet.hasNext()) {
//                QuerySolution result = countResultSet.nextSolution();
//                localCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
//            }
//            log.info("符合关联数据总条数 ：{}", localCount);
//            for (int offset1 = 0, limit1 = 10000; offset1 < localCount; offset1 += limit1) {
//                //查询本地需要动态获取的数据
//                ResultSet resultSet = RdfUtils.queryTriple(localEndPoint, localSparql + " offset " + offset1 + " limit " + limit1);
//                List<Map<String, Object>> localResults = RdfUtils.resultEncapsulation(resultSet);
//                log.info("符合关联数据 : {} 条  ", localResults.size());
//                List<String> rvs = resultSet.getResultVars();
//                String subject = rvs.get(0);
//                //每一千条数据作为一块进行处理
//                for (int offset = 0, limit = 1000; offset < localResults.size(); offset += limit) {
//                    List<Map<String, Object>> localResultSub = localResults.subList(offset, Math.min(offset + limit, localResults.size()));
//                    List<Statement> statementList = new ArrayList<>();
//                    for (int i = 0; i < localResultSub.size(); i++) {
//                        Map<String, Object> tempMap = localResultSub.get(i);
//                        String sub = String.valueOf(tempMap.get(subject));
//                        String dyf = String.valueOf(tempMap.get(dynamicFiled));
//                        String languaue = String.valueOf(tempMap.get("lang"));
//                        String contact;
//                        if (lang) {
//                            contact = "'" + StringUtil.transformSparqlStr(dyf) + "'@" + languaue;
//                        } else {
//                            contact = "'" + StringUtil.transformSparqlStr(dyf) + "'";
//                        }
//                        contact = contact.trim();
////                        String sparql = " select * where {  ?s  <http://www.w3.org/2000/01/rdf-schema#label>  " + contact.trim() + " . }";
//                      /*  String sparql = " select * where {  ?s  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . VALUES ?label { " +
//                                "'" + contact + "' '" + contact + "'@zh '" + contact + "'@en '" + contact + "'@la '" + contact + "'@ja }" +
//                                "}";*/
//                        String sparql = " select * where {  ?s  <http://www.w3.org/2000/01/rdf-schema#label>  ?label  . VALUES ?label { " +
//                                contact + " " + contact + "@zh " + contact + "@en " + contact + "@la " + contact + "@ja }" +
//                                "}";
//                        ResultSet lingResult = RdfUtils.queryTriple(linkEndPoint, sparql);
//                        if (null != lingResult) {
//                            while (lingResult.hasNext()) {
//                                String linkSubj = lingResult.getResultVars().get(0);
//                                QuerySolution solution = lingResult.nextSolution();
//                                Resource linkSub = solution.getResource(linkSubj);
//                                statementList.add(new StatementImpl(new ResourceImpl(sub), OWL.sameAs, new ResourceImpl(linkSub.getURI())));
//                            }
//                        }
//                    }
//                    VirtModel model = new VirtModel(graphVirt);
//                    log.info("存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statementList.size());
//                    model.add(statementList);
//                    log.info("已经执行完成 {} 次", k++);
//                }
//            }
//            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.89.33:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
//            String count = "";
//            //输出结果集
//            while (ontologyResult.hasNext()) {
//                QuerySolution result = ontologyResult.nextSolution();
//                count = result.get("count").asLiteral().getValue().toString();
//            }
//            VirtModel model = new VirtModel(graphVirt);
//            File file = new File(filePath);
//            if (!file.getParentFile().exists()) {
//                file.getParentFile().mkdirs();
//            }
//            RDFDataMgr.write(new FileOutputStream(filePath), model, RDFFormat.TTL);
//            Long end = System.currentTimeMillis();
//            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
////            //执行结束后清空图
////            graphVirt.clear();
//            model.close();
//        } catch (Exception e) {
//            graphVirt.close();
//            e.printStackTrace();
//        } finally {
//            graphVirt.close();
//        }
//
//    }

    @Override
    public void linkGbif() {

        //动态字段
        String dynamicFiled = "o";
        //进行关联的端点带图
        String localEndPoint = "http://10.0.82.94:8890/sparql?default-graph-uri=http://localhost/Taxon";
        //进行关联的数据查询语句
        String localSparql = "select * where { ?s   <http://www.w3.org/2000/01/rdf-schema#label>  ?o   } ";
        //gbif 路径
        String gbifUrl = "https://api.gbif.org/v1/species/match";
        //gbif url
        String contactUrl = "https://www.gbif.org/species/";
        //生成文件路径
        String filePath = "/mnt/wangzl/linkGbif.ttl";
        Long start = System.currentTimeMillis();
        //清空临时存储的数据库图
        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
        graphVirt.clear();
        try {
            int k = 1;
            Gson gson = new Gson();
            String countLocal = "select  (count(*) as ?count ) where { \n" + localSparql + " \n}";
            ResultSet countResultSet = RdfUtils.queryTriple(localEndPoint, countLocal);
            int localCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                localCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            log.info("符合关联gbif 数据总条数 ：{}", localCount);
            for (int offset1 = 0, limit1 = 10000; offset1 < localCount; offset1 += limit1) {
                //查询本地需要动态获取的数据
                ResultSet resultSet = RdfUtils.queryTriple(localEndPoint, localSparql + " offset " + offset1 + " limit " + limit1);
                List<Map<String, Object>> localResults = RdfUtils.resultEncapsulation(resultSet);
                log.info("符合关联数据 : {} 条 ", localResults.size());
                List<String> rvs = resultSet.getResultVars();
                String subject = rvs.get(0);
                //每一千条数据作为一块进行处理
                for (int offset = 0, limit = 1000; offset < localResults.size(); offset += limit) {
                    List<Map<String, Object>> localResultSub = localResults.subList(offset, Math.min(offset + limit, localResults.size()));
                    List<Statement> statementList = new ArrayList<>();
                    for (int i = 0; i < localResultSub.size(); i++) {
                        Map<String, Object> tempMap = localResultSub.get(i);
                        String sub = String.valueOf(tempMap.get(subject));
                        String dyf = String.valueOf(tempMap.get(dynamicFiled));
                        String resultStr = HttpUtil.doGet(gbifUrl + "?name=" + URLEncoder.encode(dyf));
                        Map<String, String> resultMap = gson.fromJson(resultStr, new TypeToken<Map<String, String>>() {
                        }.getType());
                        if (null != resultMap) {
                            String usageKey = resultMap.get("usageKey");
                            if (StringUtils.isNotBlank(usageKey)) {
                                statementList.add(new StatementImpl(new ResourceImpl(sub), OWL.sameAs, new ResourceImpl(contactUrl + usageKey)));
                            }

                        }
                    }
                    VirtModel model = new VirtModel(graphVirt);
                    log.info("存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statementList.size());
                    model.add(statementList);
                    log.info("已经执行完成 {} 次", k++);
                }

            }
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
//            //执行结束后清空图
//            graphVirt.clear();
            model.close();
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }
    }

    @Override
    public void animalLinkGbif() {
        String temp_graph = "http://localhost/GbifTemp";
        String virtuosoip = "10.0.89.33";
        String virtuosouser = "dba";
        String virtuosopassword = "0dabigta1357";
        //动态字段
        String dynamicFiled = "o";
        //进行关联的端点带图
        String localEndPoint = "http://ioz.semweb.csdb.cn/sparql";
        //进行关联的数据查询语句
        String localSparql = "select *  from <CoLChina_sp2000>  where { ?s   <http://www.w3.org/2000/01/rdf-schema#label>  ?o . ?s a <http://purl.obolibrary.org/obo/NCBITaxon_species>.  } ";
        //gbif 路径
        String gbifUrl = "https://api.gbif.org/v1/species/match";
        //gbif url
        String contactUrl = "https://www.gbif.org/species/";
        //生成文件路径
        String filePath = "/mnt/wangzl/animalLinkGbif.ttl";
        Long start = System.currentTimeMillis();
        //清空临时存储的数据库图
        VirtGraph graphVirt = new VirtGraph(temp_graph, "jdbc:virtuoso://" + virtuosoip + ":1111", virtuosouser, virtuosopassword);
        graphVirt.clear();
        int num = 1;
        try {
            int k = 1;
            Gson gson = new Gson();
//            String countLocal = "select  (count(*) as ?count ) where { \n" + localSparql + " \n}";
            String countLocal = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "select  (count(*) as ?count )    \n" +
                    "where {\n" +
                    "  graph <CoLChina_sp2000> {\n" +
                    "    ?s  <http://www.w3.org/2000/01/rdf-schema#label>  ?o . ?s a <http://purl.obolibrary.org/obo/NCBITaxon_species>. \n" +
                    "  }   \n" +
                    "}";
            ResultSet countResultSet = RdfUtils.queryTriple(localEndPoint, countLocal);
            int localCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                localCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            log.info("符合关联gbif 数据总条数 ：{}", localCount);
            for (int offset1 = 0, limit1 = 10000; offset1 < localCount; offset1 += limit1) {
                //查询本地需要动态获取的数据
                ResultSet resultSet = RdfUtils.queryTriple(localEndPoint, localSparql + " offset " + offset1 + " limit " + limit1);
                List<Map<String, Object>> localResults = RdfUtils.resultEncapsulation(resultSet);
                log.info("符合关联数据 : {} 条 ", localResults.size());
                List<String> rvs = resultSet.getResultVars();
                String subject = rvs.get(0);
                //每一千条数据作为一块进行处理
                for (int offset = 0, limit = 1000; offset < localResults.size(); offset += limit) {
                    List<Map<String, Object>> localResultSub = localResults.subList(offset, Math.min(offset + limit, localResults.size()));
                    List<Statement> statementList = new ArrayList<>();
                    for (int i = 0; i < localResultSub.size(); i++) {
                        log.info("执行第{}条", num);
                        num++;
                        Map<String, Object> tempMap = localResultSub.get(i);
                        String sub = String.valueOf(tempMap.get(subject));
                        String dyf = String.valueOf(tempMap.get(dynamicFiled));
                        String resultStr = HttpUtil.doGet(gbifUrl + "?name=" + URLEncoder.encode(dyf));
                        Map<String, String> resultMap = gson.fromJson(resultStr, new TypeToken<Map<String, String>>() {
                        }.getType());
                        if (null != resultMap) {
                            String usageKey = resultMap.get("usageKey");
                            if (StringUtils.isNotBlank(usageKey)) {
                                statementList.add(new StatementImpl(new ResourceImpl(sub), OWL.sameAs, new ResourceImpl(contactUrl + usageKey)));
                            }

                        }
                    }
                    VirtModel model = new VirtModel(graphVirt);
                    log.info("存储图 {} 三元组数 共计 {} 个", temp_graph, statementList.size());
                    model.add(statementList);
                    log.info("已经执行完成 {} 次", k++);
                }

            }
            log.info("录入临时库完成");
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.89.33:8890/sparql" + temp_graph, "select (count(*) as  ?count) from <" + temp_graph + "> where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
//            //执行结束后清空图
//            graphVirt.clear();
            model.close();
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }
    }

    @Override
    public void deleteGraph() {
        String graph = "http://pubchem/synonym";
        Long start = System.currentTimeMillis();
        //连接virtuoso 数据库
        VirtGraph virtGraph = new VirtGraph(graph, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
        VirtModel model = new VirtModel(virtGraph);

        try {

            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=" + graph, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            log.info("删除总数 {} 条三元组", count);
            model.removeAll();
            int temp = 1;
            if (StringUtils.isNotBlank(count)) {
                int total = Integer.parseInt(count);
                for (int offset = 0, limit = 10000; offset < total; offset += limit) {
                    Query sparql = QueryFactory.create("select * where {?s ?p ?o}  limit" + limit);
                    VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, virtGraph);
                    Model model1 = vqe.execConstruct();
                    model.remove(model1.listStatements());
                    log.info("删除次数 {}", temp++);
                }
            }

        } catch (Exception e) {
            model.close();
            virtGraph.close();
            e.printStackTrace();
        } finally {
            model.close();
            virtGraph.close();
        }
        Long end = System.currentTimeMillis();
        log.info("执行时间{} s", (end - start) / 1000);
    }

    @Override
    public void dealLinkPubchem() {
        /**
         * 实现思路从化合物基本数据图中获取到 cas 号资源 下的 cas 号得值 并且获取到挂在那个 化合物主语下
         * 拿 cas号在 http://pubchem/synonym 获取到对应 同义词主语
         * 下挂载的化合物主语在 http://pubchem/compound 中查询  rdf:type ,skos:closeMatch 数据 变成未本地化合物主语并存入到
         * http://localhost/temp 图中
         * 写入到文件
         * */
        Long start = System.currentTimeMillis();
        String filePath = "/mnt/wangzl/basiclinkPubchem.ttl";
        VirtGraph graphVirt = null;
        try {
            String countBasic = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX SIO: <http://semanticscience.org/resource/SIO_> \n" +
                    "PREFIX CHEMINF: <http://semanticscience.org/resource/CHEMINF_> \n" +
                    "select ( count(*) as ?count ) where {\n" +
                    "\t?descriptor rdf:type  CHEMINF:000446.\n " +
                    "\t?descriptor  SIO:000300 ?cas .\n" +
                    "\t?compound  SIO:000008  ?descriptor.\n }";
            ResultSet countResultSet = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=http://localhost/basic", countBasic);
            int basicCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                basicCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            for (int offset = 0, limit = 10000; offset < basicCount; offset += limit) {
                String queryBasic = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX SIO: <http://semanticscience.org/resource/SIO_> \n" +
                        "PREFIX CHEMINF: <http://semanticscience.org/resource/CHEMINF_> \n" +
                        "select  ?compound ?cas where {\n" +
                        "\t?descriptor rdf:type  CHEMINF:000446.\n " +
                        "\t?descriptor  SIO:000300 ?cas .\n" +
                        "\t?compound  SIO:000008  ?descriptor.\n } " +
                        "offset " + offset + "  limit " + limit;
                ResultSet basicResultSet = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=http://localhost/basic", queryBasic);
                while (basicResultSet.hasNext()) {
                    List<Statement> statementList = new ArrayList<>();
                    QuerySolution basic = basicResultSet.nextSolution();
                    Resource compoundSubj = basic.getResource("compound");
                    RDFNode cas = basic.get("cas");
                    //查询同义词库
                    String synonymQuery = "select ?compoundSubj where  {\n" +
                            "\t?synonymSubj <http://semanticscience.org/resource/SIO_000300> '" + cas.asLiteral().getValue().toString() + "'.\n" +
                            "\t?synonymSubj <http://semanticscience.org/resource/SIO_000011> ?compoundSubj.\n" +
                            "}";
                    ResultSet synonym = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=http://pubchem/synonym", synonymQuery);
                    if (null != synonym) {
                        while (synonym.hasNext()) {
                            QuerySolution synonymSolution = synonym.nextSolution();
                            Resource pubCompoundSubj = synonymSolution.getResource("compoundSubj");
                            //查询compound
                            String compoundQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                                    "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                                    "select ?s ?p ?o where  {\n" +
                                    "  bind (<" + pubCompoundSubj.getURI() + "> as ?s )\n" +
                                    "  ?s ?p ?o.\n" +
                                    "  FILTER ( ?p = rdf:type || ?p =skos:closeMatch )\n" +
                                    "}";
                            ResultSet compound = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=http://pubchem/compound", compoundQuery);
                            if (null != compound) {
                                while (compound.hasNext()) {
                                    QuerySolution compoundSolution = compound.nextSolution();
                                    Resource pValue = compoundSolution.getResource("p");
                                    RDFNode oValue = compoundSolution.get("o");
                                    statementList.add(new StatementImpl(compoundSubj, new PropertyImpl(pValue.asResource().getURI()), oValue));
                                }
                            }
                        }
                    }

//                //存储每个对应数据三元组
                    if (statementList.size() > 0) {
                        graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
                        VirtModel model = new VirtModel(graphVirt);
                        model.add(statementList);
                        graphVirt.close();
                    }
                }
                log.info("offset: {}", offset);
            }

            log.info("处理完成三元组数据");
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
            VirtModel model2 = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model2, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
        } catch (Exception e) {
            if (null != graphVirt) {
                graphVirt.close();
            }
            e.printStackTrace();
        } finally {
            if (null != graphVirt) {
                graphVirt.close();
            }
        }
    }

    @Override
    public void compoundLink() {
        /**
         * 化合物 sameAs pubchem
         * 化合物 sameAs dbpedia
         */

        Long start = System.currentTimeMillis();
        String filePath = "/mnt/wangzl/compoundLink.ttl";
        VirtGraph graphVirt = null;
        try {
            String countBasic = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX SIO: <http://semanticscience.org/resource/SIO_> \n" +
                    "PREFIX CHEMINF: <http://semanticscience.org/resource/CHEMINF_> \n" +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                    "select ( count(*) as ?count ) where {\n" +
                    "\t?descriptor rdf:type  CHEMINF:000446.\n " +
                    "\t?descriptor  SIO:000300 ?cas .\n" +
                    "\t?compound  SIO:000008  ?descriptor.\n  " +
                    "\t?compound   rdfs:label ?label.\n  " +
                    "}";
            ResultSet countResultSet = RdfUtils.queryTriple("http://chemdb.semweb.csdb.cn/sparql?default-graph-uri=basicInfo", countBasic);
            int basicCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                basicCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            for (int offset = 0, limit = 10000; offset < basicCount; offset += limit) {
                String queryBasic = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX SIO: <http://semanticscience.org/resource/SIO_> \n" +
                        "PREFIX CHEMINF: <http://semanticscience.org/resource/CHEMINF_> \n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "select  ?compound ?cas ?label where {\n" +
                        "\t?descriptor rdf:type  CHEMINF:000446.\n " +
                        "\t?descriptor  SIO:000300 ?cas .\n" +
                        "\t?compound  SIO:000008  ?descriptor.\n" +
                        "\t?compound   rdfs:label ?label.\n" +
                        " } " +
                        "offset " + offset + "  limit " + limit;
                ResultSet basicResultSet = RdfUtils.queryTriple("http://chemdb.semweb.csdb.cn/sparql?default-graph-uri=basicInfo", queryBasic);
                while (basicResultSet.hasNext()) {
                    List<Statement> statementList = new ArrayList<>();
                    QuerySolution basic = basicResultSet.nextSolution();
                    Resource compoundSubj = basic.getResource("compound");
                    RDFNode cas = basic.get("cas");
                    RDFNode label = basic.get("label");
                    //查询同义词库
                    String synonymQuery = "select ?compoundSubj where  {\n" +
                            "\t?synonymSubj <http://semanticscience.org/resource/SIO_000300> '" + cas.asLiteral().getValue().toString() + "'.\n" +
                            "\t?synonymSubj <http://semanticscience.org/resource/SIO_000011> ?compoundSubj.\n" +
                            "}";
                    ResultSet synonym = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=http://pubchem/synonym", synonymQuery);
                    if (null != synonym) {
                        while (synonym.hasNext()) {
                            QuerySolution synonymSolution = synonym.nextSolution();
                            Resource pubCompoundSubj = synonymSolution.getResource("compoundSubj");
                            statementList.add(new StatementImpl(compoundSubj, OWL.sameAs, pubCompoundSubj));
                        }
                    }

                    // 关联dbpedia  https://dbpedia.org/sparql   http://dbpedia.org
                    String dbpediaQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                            "select * where {\n" +
                            "   ?s  rdfs:label  '" + StringUtil.transformSparqlStr(label.asLiteral().getValue().toString()) + "'@en\n" +
                            "  filter (strStarts(str(?s),'http://dbpedia.org/resource/'))\n" +
                            "}";
                    ResultSet dbpedia = RdfUtils.queryTriple("https://dbpedia.org/sparql?default-graph-uri=http://dbpedia.org", dbpediaQuery);
                    if (null != dbpedia) {
                        while (dbpedia.hasNext()) {
                            QuerySolution dbpediaSolution = dbpedia.nextSolution();
                            Resource dbpediaSubj = dbpediaSolution.getResource("?s");
                            statementList.add(new StatementImpl(compoundSubj, OWL.sameAs, dbpediaSubj));
                        }
                    }
                    //存储每个对应数据三元组
                    if (statementList.size() > 0) {
                        graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
                        VirtModel model = new VirtModel(graphVirt);
                        model.add(statementList);
                        graphVirt.close();
                    }
                }
                log.info("offset: {}", offset);
            }
            log.info("处理完成三元组数据");
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
            VirtModel model2 = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model2, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
        } catch (Exception e) {
            if (null != graphVirt) {
                graphVirt.close();
            }
            e.printStackTrace();
        } finally {
            if (null != graphVirt) {
                graphVirt.close();
            }
        }
    }

    @Override
    public void getWikiData() {
        Long start = System.currentTimeMillis();
        //是否拼接语种
        boolean lang = true;
        //动态字段
        String dynamicFiled = "o";
        //进行关联的端点带图
        String localEndPoint = "https://www.plantplus.cn/plantsw/sparql?default-graph-uri=sp2000";
        //生成文件路径
        String filePath = "/mnt/wangzl/wikidata.ttl";
        //清空临时存储的数据库图
        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
//        graphVirt.clear();
        try {
            int k = 1;
            //查询本地需要动态获取的数据
            String cont = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "SELECT ( count(*) as ?count) WHERE {\n" +
                    "    ?s  rdf:type \t<http://rs.tdwg.org/dwc/terms/Taxon> .\n" +
                    "    ?s  rdfs:label ?obj.\n" +
                    "} ";

            ResultSet countResultSet = RdfUtils.queryTriple(localEndPoint, cont);
            int basicCount = 0;
            while (countResultSet.hasNext()) {
                QuerySolution result = countResultSet.nextSolution();
                basicCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            System.out.println("basicCount: " + basicCount);
            for (int offset1 = 0, limit1 = 10000; offset1 < basicCount; offset1 += limit1) {
                //进行关联的数据查询语句
                String localSparql = " PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> select * where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rs.tdwg.org/dwc/terms/Taxon>. ?s  rdfs:label  ?o } offset " + offset1 + " limit " + limit1;
                ResultSet resultSet = RdfUtils.queryTriple(localEndPoint, localSparql);
                List<Map<String, Object>> localResults = RdfUtils.resultEncapsulation(resultSet);
                log.info("符合关联数据 : {} 条", localResults.size());
                List<String> rvs = resultSet.getResultVars();
                String subject = rvs.get(0);
                //每一千条数据作为一块进行处理
                for (int offset = 0, limit = 1000; offset < localResults.size(); offset += limit) {
                    List<Map<String, Object>> localResultSub = localResults.subList(offset, Math.min(offset + limit, localResults.size()));
                    List<Statement> statementList = new ArrayList<>();
                    for (int i = 0; i < localResultSub.size(); i++) {
                        Map<String, Object> tempMap = localResultSub.get(i);
                        String sub = String.valueOf(tempMap.get(subject));
                        String dyf = String.valueOf(tempMap.get(dynamicFiled));
                        String languaue = String.valueOf(tempMap.get("lang"));
                        String contact;
                        if (lang) {
                            contact = "'" + StringUtil.transformSparqlStr(dyf) + "'@" + languaue;
                        } else {
                            contact = "'" + StringUtil.transformSparqlStr(dyf) + "'";
                        }
                        String sparql = "CONSTRUCT {?s  <http://www.w3.org/2000/01/rdf-schema#label>  " + contact + " .} where {  ?s  <http://www.w3.org/2000/01/rdf-schema#label>  " + contact + " . }";
//                        URIBuilder ub = new URIBuilder("https://query.wikidata.org/sparql");
//                        ub.addParameter("query", sparql);
//                        String command = "cmd /c \"curl "+ub.toString()+"\"";
                        String command = "cmd /c \"curl https://query.wikidata.org/sparql?query=" + URLEncoder.encode(sparql, "UTF-8") + "\"";
//                        System.out.println("CURL :"+command);
                        String result = ShellExecuteUtil.executeCommand(command);
                        if (StringUtils.isNotBlank(result)) {
                            Model model = ModelFactory.createDefaultModel();
                            model.read(StringUtil.getStringStream(result), "RDF/XML");
                            ResIterator resIterator = model.listSubjects();
                            while (resIterator.hasNext()) {
                                Resource wikiSub = resIterator.next();
                                statementList.add(new StatementImpl(new ResourceImpl(sub), OWL.sameAs, wikiSub));
                            }
                        }
                    }
                    VirtModel model = new VirtModel(graphVirt);
                    log.info("存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statementList.size());
                    model.add(statementList);
                    log.info("已经执行完成 {} 次", k++);
                }
            }
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File file = new File(filePath);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePath), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePath, (end - start) / 1000);
//            //执行结束后清空图
//            graphVirt.clear();
            model.close();
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }
    }

    @Override
    public void generateResearchRDF() {
        Long start = System.currentTimeMillis();
        //清空临时存储的数据库图
        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
        graphVirt.clear();
        try {
            /**
             * 思路:
             *     1、获取 xml 文件存储路径
             *     2、解析并转化 xml 成 json
             *     3、json 中提取到 研究 rdf 和 药物 rdf
             *     4、并且生成对应的 rdf
             *     5、存储到 viruoso 中 结束之后导出
             * */
            //递归出文件夹下所有的文件 并且返回绝对路径
            String docPath = "/mnt/wangzl/search_result";
            String filePathSave = "/mnt/wangzl/clinicaltrials.ttl";
            String[] contains ={"ncov" ,"covid","coronavirus disease","coronavirus infection","sars-cov-2"};
            int k = 1;
            File file = new File(docPath);
            List<String> filePaths = XmlUtils.recursionFile(file);
            log.info("{} 文件夹下共有文件 {} 个", docPath, filePaths.size());
            for (String filePath : filePaths) {
                //读取文件内容并解析 xml 转化成json
                String xmlToJsonStr = XmlUtils.convertXMLtoJSON(filePath);
                List<Statement> statements = new ArrayList<>();
                if (StringUtils.isNotBlank(xmlToJsonStr)) {
                    //根据 json 提取转化 RDF 需要的数据
                    ClinicalStudyEntity cse = XmlUtils.getClinicalStudyEntity(xmlToJsonStr);
                    //生成 RDF
                    if (null != cse) {
                        String base = "http://clinicaltrials.semweb.csdb.cn/resource/";
                        String drugBase = "http://clinicaltrials.semweb.csdb.cn/resource/Drug_";
                        //type
                        statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), RDF.type, new ResourceImpl("http://purl.obolibrary.org/obo/CTO_0000109")));
                        //身份标识
                        statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("https://schema.org/identifier"), ResourceFactory.createStringLiteral(cse.getNct())));
                        //url
                        statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("https://schema.org/url"), ResourceFactory.createStringLiteral(cse.getUrl())));
                        //label
                        if (null != cse.getBriefTitle()) {
                            statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), RDFS.label, ResourceFactory.createLangLiteral(cse.getBriefTitle(), Objects.requireNonNull(StringUtil.getCodeName(cse.getBriefTitle())).get(0))));
                        }
                        //conditions
                        if (null != cse.getConditions() && cse.getConditions().size() > 0) {
                            for (String condition : cse.getConditions()) {
                                Arrays.asList(contains).forEach((item)->{
                                    if(condition.toLowerCase().contains(item)){
                                        statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://purl.obolibrary.org/obo/NCIT_C93360"), new ResourceImpl("http://micro.semweb.csdb.cn/resource/Mesh_D000086382")));
                                    }
                                });
                                if (condition.contains("SARS-CoV-2")) {
                                    statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://purl.obolibrary.org/obo/NCIT_C93360"), new ResourceImpl("http://micro.semweb.csdb.cn/resource/Mesh_D000086402")));
                                    statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://purl.obolibrary.org/obo/NCIT_C93360"), new ResourceImpl("http://micro.semweb.csdb.cn/resource/VirusClass_SARS-CoV-2")));
                                }
                                statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://purl.obolibrary.org/obo/NCIT_C93360"), ResourceFactory.createLangLiteral(condition, Objects.requireNonNull(StringUtil.getCodeName(condition)).get(0))));
                            }
                        }
                        //locations
                        if (null != cse.getLocations() && cse.getLocations().size() > 0) {
                            for (String location : cse.getLocations()) {
                                statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://purl.obolibrary.org/obo/RO_0001025"), ResourceFactory.createLangLiteral(location, Objects.requireNonNull(StringUtil.getCodeName(location)).get(0))));
                            }
                        }
                        //briefSummary 简要总结
                        if (null != cse.getBriefSummary()) {
                            statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://www.bioassayontology.org/bao#BAO_0000812"), ResourceFactory.createLangLiteral(cse.getBriefSummary(), Objects.requireNonNull(StringUtil.getCodeName(cse.getBriefSummary())).get(0))));
                        }
                        //primaryPurpose 研究意图
                        if (null != cse.getPrimaryPurpose()) {
                            statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://www.bioassayontology.org/bao#BAO_0000211"), ResourceFactory.createLangLiteral(cse.getPrimaryPurpose(), Objects.requireNonNull(StringUtil.getCodeName(cse.getPrimaryPurpose())).get(0))));
                        }
                        //durgs
                        if (null != cse.getDrugs() && cse.getDrugs().size() > 0) {
                            for (Map<String, Object> map : cse.getDrugs()) {
                                String name = String.valueOf(map.get("name"));
                                String regEx = "[`~!@#$%^&*()\\-+={}':;,\\[\\].<>/?￥%…（）_+|【】‘；：”“’。，、？]";
                                Pattern p = Pattern.compile(regEx);
                                Matcher m = p.matcher(name);
                                name = m.replaceAll("").replaceAll(" ", "_");
                                statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://purl.obolibrary.org/obo/OAE_0000002"), new ResourceImpl(drugBase + name)));
                                statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://purl.obolibrary.org/obo/OAE_0000002"), ResourceFactory.createStringLiteral(String.valueOf(map.get("name")))));
                                statements.add(new StatementImpl(new ResourceImpl(drugBase + name), RDF.type, new ResourceImpl("http://purl.obolibrary.org/obo/CHEBI_23888")));
                                statements.add(new StatementImpl(new ResourceImpl(drugBase + name), RDFS.label, ResourceFactory.createLangLiteral(String.valueOf(map.get("name")), Objects.requireNonNull(StringUtil.getCodeName(String.valueOf(map.get("name")))).get(0))));
                                if (null != map.get("otherName")) {
                                    if (map.get("otherName") instanceof ArrayList<?>) {
                                        for (Object otherName : (List<?>) map.get("otherName")) {
                                            statements.add(new StatementImpl(new ResourceImpl(drugBase + name), new PropertyImpl("http://purl.obolibrary.org/obo/VO_0003099"), ResourceFactory.createLangLiteral(String.valueOf(otherName), Objects.requireNonNull(StringUtil.getCodeName(String.valueOf(otherName))).get(0))));
                                        }
                                    }
                                }
                                statements.add(new StatementImpl(new ResourceImpl(base + cse.getNct()), new PropertyImpl("http://purl.obolibrary.org/obo/CIDO_0000012"), ResourceFactory.createLangLiteral(String.valueOf(map.get("description")), Objects.requireNonNull(StringUtil.getCodeName(String.valueOf(map.get("description")))).get(0))));
                                //关联化合物
                                String sparqlStr = "SELECT * WHERE {  ?s <http://www.w3.org/2000/01/rdf-schema#label> '" + StringUtil.transformSparqlStr(String.valueOf(map.get("name"))) + "' .}";
                                ResultSet resultSet = RdfUtils.queryTriple("http://chemdb.semweb.csdb.cn/sparql?default-graph-uri=basicInfo", sparqlStr);
                                if (null != resultSet) {
                                    while (resultSet.hasNext()) {
                                        QuerySolution solution = resultSet.nextSolution();
                                        Resource subj = solution.getResource("s");
                                        statements.add(new StatementImpl(new ResourceImpl(drugBase + name), new PropertyImpl("http://purl.obolibrary.org/obo/CIDO_0000022"), subj));
                                    }
                                }
                                //关联pubchem
                                String query = "select ?o where { ?s ?p '"+StringUtil.transformSparqlStr(String.valueOf(map.get("name"))).toLowerCase()+"' . ?s <http://semanticscience.org/resource/SIO_000011> ?o }";
                                ResultSet  queryResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=http://pubchem/synonym", query);
                                if (null != queryResult) {
                                    while (queryResult.hasNext()) {
                                        QuerySolution solution = queryResult.nextSolution();
                                        Resource subj = solution.getResource("o");
                                        statements.add(new StatementImpl(new ResourceImpl(drugBase + name), new PropertyImpl("http://purl.obolibrary.org/obo/CIDO_0000022"), subj));
                                    }
                                }
                            }
                        }
                    }
                }
                VirtModel model = new VirtModel(graphVirt);
                log.info("存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statements.size());
                model.add(statements);
                log.info("已经执行完成 {} 次", k++);
            }
            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File files = new File(filePathSave);
            if (!files.getParentFile().exists()) {
                files.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePathSave), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePathSave, (end - start) / 1000);
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }
    }

    @Override
    public void generateNCMIRDF() {
        /**
         * 生成人口健康思路
         * 1、获取人口健康的 Clinical trail 和drug 数据
         * 2、生成 对应的 rdf 数据  Clinical trail Intervention 字段拆分 drug 数据兵
         *    关联固定 微生物数据
         * 3、drug 数据 根据 名称关联到 化合物 上 不区分大小写 同样关联固定微生物数据
         * */
        Long start = System.currentTimeMillis();
        //清空临时存储的数据库图
        VirtGraph graphVirt = new VirtGraph(TEMP_GRAPH, "jdbc:virtuoso://" + virtuosoIp + ":1111", virtuosoUser, virtuosoPassword);
        graphVirt.clear();
        try {
            String filePathSave = "/mnt/wangzl/ncmi.ttl";
            //Clinical trail  rdf
            String nctUrl = "https://www.ncmi.cn/covid-19/searchListShare.do?page=1&classen=clinical_&searchText=&searchText2=&sort=language+desc;date+desc;id+desc;&sessionLang=english_en&pageNumber=10927";
            String nctBase = "http://ncmi.semweb.csdb.cn/resource/";
            String diseaseBase = "http://ncmi.semweb.csdb.cn/resource/Disease_";
            String drugBase = "http://ncmi.semweb.csdb.cn/resource/Drug_";

            List<NcmiClinicalTrail> nctList = FindLinksHelper.getNcmiTrail(nctUrl);
            if (null != nctList && nctList.size() > 0) {
                for (NcmiClinicalTrail nct : nctList) {
                    List<Statement> statements = new ArrayList<>();
                    String nctNumber = nct.getNctNumber();
                    if (StringUtils.isNotBlank(nctNumber)) {
                        String sparqlNctNum = "select * where { ?s <https://schema.org/identifier> '" + nctNumber + "'}";
                        ResultSet identifierResultSet = RdfUtils.queryTriple("http://10.0.82.94:8890/sparql/?default-graph-uri=Clinical_Trials", sparqlNctNum);
                        if (null != identifierResultSet && identifierResultSet.hasNext()) {
                            continue;
                        }
                        AtomicBoolean typeFlag = new AtomicBoolean(true);
                        if(null !=  nct.getInterventions()){
                            nct.getInterventions().forEach((item) -> {
                                if (item.contains("Drug:")) {
                                    typeFlag.set(false);
                                    statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), RDF.type, new ResourceImpl("http://purl.obolibrary.org/obo/CTO_0000109")));
                                    //拆分 drug 生成 drug
                                    List<String> list = FindLinksHelper.splitDrugStr(item);
                                    for (String str : list) {
                                        String newStr = str.replaceAll(" ", "_");
                                        statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), new PropertyImpl("http://purl.obolibrary.org/obo/OAE_0000002"), new ResourceImpl(drugBase + newStr)));
                                        statements.add(new StatementImpl(new ResourceImpl(drugBase + newStr), RDF.type, new ResourceImpl("http://purl.obolibrary.org/obo/CHEBI_23888")));
                                        statements.add(new StatementImpl(new ResourceImpl(drugBase + newStr), RDFS.label, ResourceFactory.createLangLiteral(str, Objects.requireNonNull(StringUtil.getCodeName(str)).get(0))));
                                    }
                                }
                                //无论 interventions 是否包含 drug  都使用竖线分割并且截取分号之后的 加上
                                String[] items = item.split("\\|");
                                for (String str : items) {
                                    str = str.substring(str.indexOf(":") + 1).trim();
                                    statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), new PropertyImpl("http://purl.obolibrary.org/obo/OAE_0000002"), ResourceFactory.createLangLiteral(str, Objects.requireNonNull(StringUtil.getCodeName(str)).get(0))));
                                }
                            });
                        }
                        statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), RDFS.label, ResourceFactory.createLangLiteral(nct.getTitle(), Objects.requireNonNull(StringUtil.getCodeName(nct.getTitle())).get(0))));

                        if (typeFlag.get()) {
                            statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), RDF.type, new ResourceImpl("http://purl.obolibrary.org/obo/CTO_0000220")));
                        }
                        statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), new PropertyImpl("https://schema.org/identifier"), ResourceFactory.createStringLiteral(nctNumber)));
                        if (null != nct.getConditions() && nct.getConditions().size() > 0) {
                            for (String condition : nct.getConditions()) {
                                condition = condition.replaceAll("\n","").replaceAll("<br>","").trim();
                                statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), new PropertyImpl("http://purl.obolibrary.org/obo/NCIT_C93360"), ResourceFactory.createStringLiteral(condition)));
                                //创建疾病实体
                                String regEx = "[`~!@#$%^&*()\\-+={}':;,\\[\\].<>/?￥%…（）_+|【】‘；：”“’。，、？\"\"]";
                                Pattern p = Pattern.compile(regEx);
                                Matcher m = p.matcher(condition);
                                String  conditionNew = m.replaceAll("").replaceAll(" ", "_");
                                statements.add(new StatementImpl(new ResourceImpl(diseaseBase + conditionNew), RDF.type, new ResourceImpl("http://purl.obolibrary.org/obo/OGMS_0000031")));
                                statements.add(new StatementImpl(new ResourceImpl(diseaseBase + conditionNew), RDFS.label, ResourceFactory.createLangLiteral(condition, Objects.requireNonNull(StringUtil.getCodeName(condition)).get(0))));
                            }
                        }
                        if (null != nct.getLocations()) {
                            //locations 使用都好分割处理
                            statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), new PropertyImpl("http://purl.obolibrary.org/obo/RO_0001025"), ResourceFactory.createLangLiteral(nct.getLocations(), Objects.requireNonNull(StringUtil.getCodeName(nct.getLocations())).get(0))));
                        }
                        if (null != nct.getUrl()) {
                            statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), new PropertyImpl("https://schema.org/url"), ResourceFactory.createTypedLiteral(nct.getUrl(), XSDanyURI)));
                        }
                        //固定关联
                        statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), new PropertyImpl("http://semanticscience.org/resource/SIO_000001"), new ResourceImpl("http://micro.semweb.csdb.cn/resource/Mesh_D000086382")));
                        statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), new PropertyImpl("http://semanticscience.org/resource/SIO_000001"), new ResourceImpl("http://micro.semweb.csdb.cn/resource/Mesh_D000086402")));
                        statements.add(new StatementImpl(new ResourceImpl(nctBase + nctNumber), new PropertyImpl("http://semanticscience.org/resource/SIO_000001"), new ResourceImpl("http://micro.semweb.csdb.cn/resource/VirusClass_SARS-CoV-2")));
                    }
                    VirtModel model = new VirtModel(graphVirt);
                    log.info("ClinicalTrail 存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statements.size());
                    model.add(statements);
                }
            }
            //生成 drug RDF
            String ncdUrl = "https://www.ncmi.cn/covid-19/searchListShare.do?page=1&classen=medicine&searchText=&searchText2=&sort=language+desc;date+desc;id+desc;&sessionLang=english_en&pageNumber=61";
            List<NcmiClinicalDrug> ncdList = FindLinksHelper.getNcmiDrug(ncdUrl);
            if (null != ncdList && ncdList.size() > 0) {
                for (NcmiClinicalDrug ncd : ncdList) {
                    List<Statement> statements = new ArrayList<>();
                    String drugName = ncd.getTitle();
                    if (StringUtils.isNotBlank(drugName)) {
                        String drugNameNew = drugName.replaceAll(" ", "_");
                        statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), RDF.type, new ResourceImpl("http://purl.obolibrary.org/obo/CHEBI_23888")));
                        statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), RDFS.label, ResourceFactory.createLangLiteral(drugName, Objects.requireNonNull(StringUtil.getCodeName(drugName)).get(0))));
                        if (null != ncd.getDrugBankNum()) {
                            statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("https://schema.org/identifier"), ResourceFactory.createStringLiteral(ncd.getDrugBankNum())));
                        }
                        if (null != ncd.getMechanism()) {
                            statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("http://purl.obolibrary.org/obo/NCIT_R124"), ResourceFactory.createLangLiteral(ncd.getMechanism(), Objects.requireNonNull(StringUtil.getCodeName(ncd.getMechanism())).get(0))));
                        }
                        if (null != ncd.getReference()) {
                            statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("https://w3id.org/reproduceme#reference"), ResourceFactory.createLangLiteral(ncd.getReference(), Objects.requireNonNull(StringUtil.getCodeName(ncd.getReference())).get(0))));
                        }
                        if (null != ncd.getSourceUrl()) {
                            statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("https://schema.org/url"), ResourceFactory.createTypedLiteral(ncd.getSourceUrl(), XSDanyURI)));
                        }
                        if (null != ncd.getAttachment()) {
                            String imgUrl = "https://www.ncmi.cn" + ncd.getAttachment();
                            statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("https://schema.org/image"), ResourceFactory.createTypedLiteral(imgUrl, XSDanyURI)));
                        }
                        //固定关联
                        statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("http://semanticscience.org/resource/SIO_000001"), new ResourceImpl("http://micro.semweb.csdb.cn/resource/Mesh_D000086382")));
                        statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("http://semanticscience.org/resource/SIO_000001"), new ResourceImpl("http://micro.semweb.csdb.cn/resource/Mesh_D000086402")));
                        statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("http://semanticscience.org/resource/SIO_000001"), new ResourceImpl("http://micro.semweb.csdb.cn/resource/VirusClass_SARS-CoV-2")));
                        //关联化合物
                        String sparqlStr = "SELECT * WHERE {  ?s <http://www.w3.org/2000/01/rdf-schema#label> '" + StringUtil.transformSparqlStr(drugName) + "' . }";
                        ResultSet resultSet = RdfUtils.queryTriple("http://chemdb.semweb.csdb.cn/sparql?default-graph-uri=basicInfo", sparqlStr);
                        if (null != resultSet) {
                            while (resultSet.hasNext()) {
                                QuerySolution solution = resultSet.nextSolution();
                                Resource subj = solution.getResource("s");
                                statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("http://purl.obolibrary.org/obo/CIDO_0000022"), subj));
                            }
                        }
                        //关联 pubchem
                        String query = "select ?o where { ?s ?p '"+StringUtil.transformSparqlStr(drugName).toLowerCase()+"' . ?s <http://semanticscience.org/resource/SIO_000011> ?o }";
                        ResultSet  queryResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=http://pubchem/synonym", query);
                        if (null != queryResult) {
                            while (queryResult.hasNext()) {
                                QuerySolution solution = queryResult.nextSolution();
                                Resource subj = solution.getResource("o");
                                statements.add(new StatementImpl(new ResourceImpl(drugBase + drugNameNew), new PropertyImpl("http://purl.obolibrary.org/obo/CIDO_0000022"), subj));
                            }
                        }
                    }
                    VirtModel model = new VirtModel(graphVirt);
                    log.info("Clinical drug 存储图 {} 三元组数 共计 {} 个", TEMP_GRAPH, statements.size());
                    model.add(statements);
                }
            }

            ResultSet ontologyResult = RdfUtils.queryTriple("http://10.0.85.83:8890/sparql?default-graph-uri=" + TEMP_GRAPH, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            VirtModel model = new VirtModel(graphVirt);
            File files = new File(filePathSave);
            if (!files.getParentFile().exists()) {
                files.getParentFile().mkdirs();
            }
            RDFDataMgr.write(new FileOutputStream(filePathSave), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePathSave, (end - start) / 1000);
        } catch (Exception e) {
            graphVirt.close();
            e.printStackTrace();
        } finally {
            graphVirt.close();
        }
    }

    @Override
    public void generateByMesh() {
        /**
         * 生成思路
         *   1、获取端点中化合物的所有CAS 号
         *   2、将 cas 号  和对应的化合物主语传入到多线程中
         *   3、查询 mesh 对外的sparql 端点 拼接查询语句 生成对应的关系
         *   存入到 fuseki 中 不使用 virtuoso 的原因是 多线程调用 直接链接有造成连接失败的可能
         *   故而使用fuseki 使用调用端点存储
         * */

        /**
         * TODO 陈锟
         * 生成思路
         *   1、去化合物的端点中，分页获取端点中化合物的所有?subj ?cas
         *   2、去mesh的端点中，将每组?subj ?cas组装成SPARQL语句，查询化合物关联的mesh URI
         *   3、将mesh查询结果组装RDF三元组，批量保存到fuseki中的指定图中
         *   4. 将fuseki中的三元组导出到ttl文件中
         * */
        String fusekiIp ="http://10.0.85.83:3030/mjb"; // TODO 陈锟 在fuseki中新建一个数据集，语句见微盘。注意，用完后删除fuseki中的数据集，因为fuseki平时会占用较大内存
        String endpoint ="http://10.0.85.83:3030/mjb?default-graph-uri=http://localhost/ChemSameAsMesh"; // TODO endpoint = {fusekiIp}?default-graph-uri={fusekiGraph}
        String fusekiGraph ="http://localhost/ChemSameAsMesh"; // TODO 陈锟 graph名称，可以不用改
        String chemEndPoint = "http://organchem.semweb.csdb.cn/sparql"; // TODO 陈锟 改为 有机所端点 http://organchem.semweb.csdb.cn/sparql
        Long start = System.currentTimeMillis();
        try {
            String base_ = "select ?subj ?cas where { \n " +
//                    "\t bind(<http://chemdb.semweb.csdb.cn/resource/Descriptor_%2810R%29-8-acetyl-10-%28%28%282R%2C3R%2C4S%2C5R%29-4-amino-3-hydroxy-5-methyltetrahydrofuran-2-yl%29oxy%29-6%2C8%2C11-trihydroxy-1-methoxy-7%2C8%2C9%2C10-tetrahydrotetracene-5%2C12-dione%20hydrochloride_CAS_registry_number> as ?s). \n" +
                    "\t?s  <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://semanticscience.org/resource/CHEMINF_000446>. \n" +
                    "\t?s <http://semanticscience.org/resource/SIO_000300> ?cas. \n" +
                    "\t?subj <http://semanticscience.org/resource/SIO_000008> ?s.\n" +
                    "}";
            String base = """
                    select ?subj ?cas
                    where {
                      ?subj a <http://purl.obolibrary.org/obo/CHEBI_24431> .
                      ?subj <http://organchem.semweb.csdb.cn/ontology/CAS> ?cas .
                    }
                    """;
            /*
             TODO 陈锟，xxx 自行替换为有机所中cas对应的属性URI
                SELECT ?subj ?cas WHERE {
                  ?subj a <http://purl.obolibrary.org/obo/CHEBI_24431> .
                  ?subj <http://xxx/cas> ?cas .
                }
             */

            //查询所有符合的化合物数据
            String countSparql = "select (count(*) as ?count) {" + base + "}";
            ResultSet resultSet = RdfUtils.queryTriple(chemEndPoint,countSparql);
            int basicCount = 0;
            while (resultSet.hasNext()) {
                QuerySolution result = resultSet.nextSolution();
                basicCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
            }
            log.info("化合物总数: {}",basicCount);
            int k = 1;
            for (int offset =  0, limit =200; offset < basicCount; offset += limit) { // TODO 陈锟 注意调整 offset、limit
                String chemSparql =  base  + "offset " + offset + "  limit " + limit;
//                ThreadUtil.chemSameAsMeshCalling(chemSparql,fusekiIp,fusekiGraph,chemEndPoint);
//                if(   offset != 0 && offset % 10000 == 0){
//                    log.info("offset : {}" ,offset);
//                }
                //查询化合端点
                ResultSet chemResultSet = RdfUtils.queryTriple(chemEndPoint,chemSparql);
                List<Statement> statements = new ArrayList<>();
                Map<String,String> map = new HashMap<>();
                if(null != chemResultSet){
                    while (chemResultSet.hasNext()){
                        QuerySolution querySolution  = chemResultSet.nextSolution();
                        Resource chemSubj = querySolution.getResource("subj");
                        RDFNode  cas = querySolution.get("cas");
                        map.put(cas.asLiteral().getValue().toString(),chemSubj.getURI());
                    }
                }
                StringBuilder unionSparql = new StringBuilder();
                for (Map.Entry<String,String> entry:map.entrySet()) {
                    unionSparql.append("{ bind(<").append(entry.getValue())
                            .append("> as ?chem). ?mesh <http://id.nlm.nih.gov/mesh/vocab#preferredConcept> ?preferredConcept . ?preferredConcept <http://id.nlm.nih.gov/mesh/vocab#relatedRegistryNumber> ?object. filter (contains(?object,'")
                            .append(entry.getKey()).append("')).  }  union "); // TODO 这里好像拼得不对，感觉多拼了一个` (`需要检查下
                }
                String meshSparql = "SELECT ?chem ?mesh from  <http://id.nlm.nih.gov/mesh> WHERE {\n" +
                        unionSparql.substring(0,unionSparql.lastIndexOf("}")+1)+
                        "}";
                /*
                    TODO 陈锟 解释一下，查询语句如下：
                    SELECT ?chem ?mesh from  <http://id.nlm.nih.gov/mesh> WHERE {
                        {
                            bind(<?subj> as ?chem).
                            ?mesh <http://id.nlm.nih.gov/mesh/vocab#preferredConcept> ?preferredConcept .
                            ?preferredConcept <http://id.nlm.nih.gov/mesh/vocab#relatedRegistryNumber> ?object.
                            filter (contains(?object,'?cas')).
                        }
                        union
                        ......
                    }
                 */
                ResultSet meshResult = RdfUtils.queryTriple("https://id.nlm.nih.gov/mesh/sparql",meshSparql);
                if(null != meshResult){
                    while (meshResult.hasNext()){
                        QuerySolution meshSolution = meshResult.nextSolution();
                        Resource mesSubj = meshSolution.getResource("mesh");
                        Resource chemSubj = meshSolution.getResource("chem");
                        statements.add(new StatementImpl(chemSubj, OWL.sameAs,mesSubj));
                    }
                }
                RdfUtils.saveTriPle(statements,fusekiIp,fusekiGraph);
                log.info(" 存储图完成  {} 三元组数 共计 {} 个", fusekiGraph, statements.size());
                log.info("执行次数 {}", k++);
            }
//            ThreadUtil.getExecutorChem();
            //导出到文件中
            ResultSet ontologyResult = RdfUtils.queryTriple(endpoint, "select (count(*) as  ?count) where { ?s ?p ?o}");
            String count = "";
            //输出结果集
            while (ontologyResult.hasNext()) {
                QuerySolution result = ontologyResult.nextSolution();
                count = result.get("count").asLiteral().getValue().toString();
            }
            String filePathSave = "/mnt/mjb/MedNPsSameAsMesh.ttl"; // TODO
            Model model = RdfUtils.queryAllGraph(fusekiIp, fusekiGraph);
            RDFDataMgr.write(new FileOutputStream(filePathSave), model, RDFFormat.TTL);
            Long end = System.currentTimeMillis();
            log.info("查找到新的关系 {} 个, 已经写入到 {} 文件中 , 程序执行时间 {} s", count, filePathSave, (end - start) / 1000);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
