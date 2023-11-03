package com.linkeddata.portal.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Rdf 工具类
 *
 * @author wangzhiliang
 */
@Slf4j
public class RdfUtils {
    /**
     * 要连接的服务器ip
     */
    private static final String IP = "10.0.85.83";
    /**
     * 要连接的服务器端口
     */
    private static final Integer PORT = 22;
    /**
     * 要连接的服务器账号
     */
    private static final String USERNAME = "root";
    /**
     * 要连接的服务器密码
     */
    private static final String PASSWORD = "bigdata@~!2030";

    /**
     * 封装 返回结果 成一个list <Map>
     *
     * @param resultSet sparql结果集
     * @result List<Map < String, Object>>  封装的对象
     * @author wangzhiliang
     */
    public static List<Map<String, Object>> resultEncapsulation(ResultSet resultSet) {
        List<Map<String, Object>> resultList = new LinkedList<>();
        List<String> names = resultSet.getResultVars();
        while (resultSet.hasNext()) {
            Map<String, Object> map = new LinkedHashMap<>();
            QuerySolution solution = resultSet.nextSolution();
//            Iterator<String> names = solution.varNames();
            for (String name : names) {
//            while (names.hasNext()){
//                String name = names.next();
                RDFNode value = solution.get(name);
                if (null != value) {
                    map.put(name, value.isLiteral() ? value.asLiteral().getValue() : value.asResource().getURI().trim());
                    if (value.isLiteral()) {
                        map.put("lang", value.asLiteral().getLanguage());
                    }
                } else {
                    //增加如果该字段没有取到值存储一个空占位 前端动态展示表格
                    map.put(name, "");
                }
            }
            resultList.add(map);
        }
        return resultList;
    }

    /**
     * 查询语句增加命名空间
     *
     * @author wanghzhiliang
     */
    public static StringBuilder setPreFix(StringBuilder stringBuilder) {
        //化学
        stringBuilder.append("PREFIX compound: 	<http://chemdb.semweb.csdb.cn/resource/compound/> \n");
        stringBuilder.append("PREFIX ontology: 	<http://chemdb.semweb.csdb.cn/resourceontology/>  \n");
        stringBuilder.append("PREFIX descriptor: 	<http://chemdb.semweb.csdb.cn/resourcedescriptor/>  \n");
        stringBuilder.append("PREFIX reference: 	<http://chemdb.semweb.csdb.cn/resource/reference/>  \n");
        //植物
        stringBuilder.append("PREFIX Taxon: <https://www.plantplus.cn/plantsw/resource/Taxon/> \n");
        stringBuilder.append("PREFIX PreservedSpecimen: <https://www.plantplus.cn/plantsw/resource/PreservedSpecimen/> \n");
        stringBuilder.append("PREFIX Event: <https://www.plantplus.cn/plantsw/resource/Event/> \n");
        stringBuilder.append("PREFIX MachineObservation: <https://www.plantplus.cn/plantsw/resource/MachineObservation/>  \n");
        stringBuilder.append("PREFIX dwc: <http://rs.tdwg.org/dwc/terms/> \n");
        stringBuilder.append("PREFIX dwciri: <http://rs.tdwg.org/dwc/iri/> \n");
        // 公共命名空间
        stringBuilder.append("PREFIX SIO: <http://semanticscience.org/resource/SIO_>  \n");
        stringBuilder.append("PREFIX CHEMINF: 	<http://semanticscience.org/resource/CHEMINF_>  \n");
        stringBuilder.append("PREFIX CHEBI: 	<http://purl.obolibrary.org/obo/CHEBI_>  \n");
        stringBuilder.append("PREFIX xsd:	<http://www.w3.org/2001/XMLSchema#> \n");
        stringBuilder.append("PREFIX rdf:	<http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n");
        stringBuilder.append("PREFIX rdfs:	<http://www.w3.org/2000/01/rdf-schema#> \n");
        return stringBuilder;
    }

    /**
     * 保存三元组
     *
     * @param statementList 三元组对象集合
     * @author wangzhiliang
     */
    public static void saveTriPle(List<Statement> statementList, String endPoint, String graph) {
        try (RDFConnection conn = RDFConnectionRemote.service(endPoint).build()) {
            for (int offset = 0, limit = 10000; offset < statementList.size(); offset += limit) {
                Model model = ModelFactory.createDefaultModel();
                model.add(statementList.subList(offset, Math.min(offset + limit, statementList.size())));
                conn.load(graph, model);
            }
        }
    }

    /**
     * query triples 使用该方法需要进行判空
     *
     * @param endPoint
     * @param sparql   check sentence
     * @author wangzhiliang
     */
    public static ResultSet queryTriple(String endPoint, String sparql) {
        ResultSet resultSet = null;
        QueryExecution queryExecution = null;
        RDFConnection conn = null;
        try {
            conn = RDFConnectionRemote.service(endPoint).build();
            queryExecution = conn.query(sparql);
            resultSet = ResultSetFactory.copyResults(queryExecution.execSelect());
            queryExecution.close();
        } catch (Exception e) {
            System.out.println("执行异常错误的 sparql " + sparql);
            e.printStackTrace();
        } finally {
            if(null != queryExecution){
                queryExecution.close();
            }
            if (null != conn) {
                conn.close();
            }
        }
        return resultSet;
    }

    /**
     * query triples
     *
     * @param endPoint
     * @param sparql   check sentence
     * @author wangzhiliang
     */
    public static long countTriple(String endPoint, String sparql) {
        long count;
        QueryExecution queryExecution;
        try (RDFConnection conn = RDFConnectionRemote.service(endPoint).build()) {
            queryExecution = conn.query(sparql);
            count = queryExecution.execSelect().nextSolution().getLiteral("count").getLong();
        }
        return count;
    }

    /**
     * 查询图中所有三元组数量
     *
     * @param sparqlEndpoint SPARQL端点
     * @param graphName
     */
    public static long count(String sparqlEndpoint, String graphName) {
        String sparql = "SELECT ( COUNT(*) AS ?count ) WHERE { GRAPH <" + graphName + ">{ ?s ?p ?o} }";
        return countTriple(sparqlEndpoint, sparql);
    }

    /**
     * 删除整个图的三元组
     * 传入的sparql 需要加入 update
     *
     * @param sparqlEndpoint SPARQL端点
     * @param graphName
     */
    public static void deleteTriple(String sparqlEndpoint, String graphName) {
        try (RDFConnection conn = RDFConnectionRemote.service(sparqlEndpoint).build()) {
            conn.delete(graphName);
        }
    }
    /**
     * 查询 整个图
     * 传入的sparql 需要加入 update
     *
     * @param sparqlEndpoint SPARQL端点
     * @param graphName
     * @return Model
     */
    public static Model queryAllGraph(String sparqlEndpoint, String graphName) {
        Model model = null;
        try (RDFConnection conn = RDFConnectionRemote.service(sparqlEndpoint).build()) {
            model = conn.fetch(graphName);
        }catch (Exception e){
            log.info("查询全图数据异常");
            e.printStackTrace();
        }
        return model;
    }

    /**
     * 存储公共数据
     *
     * @author wangzhiliang
     */
    public static void savePublic(String path, String endPoint, String graph) {
        try (RDFConnection conn = RDFConnectionRemote.service(endPoint).build()) {
            Model model = ModelFactory.createDefaultModel();
            model.read(new FileInputStream(path), null, "TTL");
            conn.load(graph, model);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String endpoint = "http://10.0.85.83:8890/sparql/?default-graph-uri=http://localhost/temp";
        String filePathSave = "D:\\mnt\\clinicaltrials_drug.ttl";

        String queryStr = "select  ?s ?p ?o WHERE { \n" +
                "  ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.obolibrary.org/obo/CHEBI_23888>.\n" +
                "  ?s ?p ?o.\n" +
                "}";
        String queryCount = "select (count(*) as ?count) where {"+queryStr+"}";
        ResultSet count = queryTriple(endpoint, queryCount);
        int localCount = 0;
        while (count.hasNext()) {
            QuerySolution result = count.nextSolution();
            localCount = Integer.parseInt(result.get("count").asLiteral().getValue().toString());
        }
        try {
            List<Statement> statements = new LinkedList<>();
            for (int offset = 0, limit = 10000; offset < localCount; offset += limit) {
                ResultSet resultSet = queryTriple(endpoint, queryStr+ " offset " + offset + " limit " + limit);
                if (null != resultSet) {
                    while (resultSet.hasNext()) {
                        QuerySolution querySolution = resultSet.nextSolution();
                        Resource subj = querySolution.getResource("s");
                        Resource pred = querySolution.getResource("p");
                        RDFNode obj = querySolution.get("o");
                        statements.add(new StatementImpl(subj, new PropertyImpl(pred.getURI()), obj));
                    }
                }
            }
            Model model = ModelFactory.createDefaultModel();
            model.add(statements);
            RDFDataMgr.write(new FileOutputStream(filePathSave), model, RDFFormat.TTL);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Query query = QueryFactory.create(queryStr);
//        try (RDFConnection conn = RDFConnectionRemote.service(endpoint).build()) {
//            Model resultModel  = conn.queryConstruct(query);
//            RDFDataMgr.write(new FileOutputStream(filePathSave), resultModel, RDFFormat.TTL);
//        }catch (Exception e){
//            e.printStackTrace();
//        }


    }

    /**
     * 执行SPARQL Construct联邦查询；
     * TODO 陈锟 临时用，将来会使用方法sparqlConstructWithEndpointsNew
     * @param sparql
     * @param endpoints SPARQL端点list，英文逗号隔开。例如：http://endpoint1,http://endpoint2
     * @return Model
     * @author 陈锟
     * @date 2023年3月30日15:37:57
     */
    public static Model sparqlConstructWithEndpoints(String sparql, String endpoints) {
        return RdfUtils.sparqlConstruct(sparql);
    }

    /**
     * 执行SPARQL Select联邦查询；
     * TODO 陈锟 临时用，将来会使用方法sparqlSelectWithEndpointsNew
     * @param sparql
     * @param endpoints SPARQL端点list，英文逗号隔开。例如：http://endpoint1,http://endpoint2
     * @return Model
     * @author 陈锟
     * @date 2023年3月30日15:37:57
     */
    public static ResultSet sparqlSelectWithEndpoints(String sparql, String endpoints) {
        return RdfUtils.sparqlSelect(sparql);
    }

//    /**
//     * 执行SPARQL Construct联邦查询
//     * @param sparql
//     * @param endpoints SPARQL端点list，英文逗号隔开。例如：http://endpoint1,http://endpoint2
//     * @return Model
//     * @author 陈锟
//     * @date 2023年3月27日10:59:42
//     */
//    public static Model sparqlConstructWithEndpointsNew(String sparql, String endpoints) {
//        // 调用‘comunica-query-sparql’执行SPARQL联邦查询
//        // 将返回结果封装
////        String nTriplesString = "<http://example.org/resource1> <http://example.org/property> <http://example.org/resource2> ."; // TODO ‘comunica-query-sparql’的返回内容
////        try (InputStream in = new ByteArrayInputStream(nTriplesString.getBytes())) {
////            Model model = ModelFactory.createDefaultModel();
////            RDFDataMgr.read(model, in, Lang.NTRIPLES);
////            // TODO 可用下面的方法验证结果
////            model.write(System.out, RDFFormat.NTRIPLES.getLang().getLabel());
////            return model;
////        } catch (IOException e) {
////            throw new RuntimeException(e);
////        }
//        // TODO 高帅 改为调用联邦查询引擎，请移除下面的2行代码，并放开上面的注释
//        Date date1 = new Date();
//        String points = endpoints.replaceAll(",", " ");
////        String cmd = "docker run --name comunica_" + System.currentTimeMillis() + "  --rm comunica/query-sparql " + points + " \" " + sparql + " \" -t 'application/n-triples' ";
//        String cmd = "docker exec comunica comunica-sparql " + points + " \" " + sparql + " \" -t 'application/n-triples' ";
//        ExecuteShellUtil instance = ExecuteShellUtil.getInstance();
//        Model model = ModelFactory.createDefaultModel();
//        try {
//            instance.init(IP, PORT, USERNAME, PASSWORD);
//        } catch (JSchException e) {
//            log.info("连接失败");
//            e.printStackTrace();
//        }
//        String result = "";
//        try {
//            System.out.println("执行联邦查询：\n```\n" + cmd + "\n```"); // 陈锟本机测试用
//            result = instance.execCmd(cmd);
//            List<String> lineFreedList = StrSplitter.splitByRegex(StrUtil.trimToEmpty(result), "\n", -1, true, true);
//            for (String s : lineFreedList) {
//                List<String> stringList = StrSplitter.split(StrUtil.trimToEmpty(s), " ", 4, true, true);
//                if (null != stringList && !stringList.isEmpty()) {
//                    String subject = stringList.get(0).replace("<", "").replace(">", "");
//                    String predicate = stringList.get(1).replace("<", "").replace(">", "");
//                    String object = stringList.get(2);
//                    Property subjectIri = model.createProperty(subject);
//                    Property predicateIri = model.createProperty(predicate);
//                    Property objectIri;
//                    Statement statement;
//                    // 宾语如果是IRI则去掉<>。字面量则不改变
//                    if (object.contains("<") || object.contains(">")) {
//                        object = stringList.get(2).replace("<", "").replace(">", "");
//                        objectIri = model.createProperty(object);
//                        statement = model.createStatement(subjectIri, predicateIri, objectIri);
//
//                    } else {
//                        statement = model.createStatement(subjectIri, predicateIri, object);
//                    }
//                    model.add(statement);
//                }
//            }
//            Date date2 = new Date();
//            System.out.println("联邦查询执行完成，用时：" + (date2.getTime() - date1.getTime()) + "毫秒"); // 陈锟本机测试用
//            if(model == null || model.size() == 0){
//                throw new Exception();
//            }
//            return model;
//        } catch (Exception e) {
//            System.out.println("联邦查询执行失败"); // 陈锟本机测试用
//            log.info("error info");
//            e.printStackTrace();
//            return model;
//        } finally {
//            ExecuteShellUtil.closeConnect();
//        }
//    }
//
//    /**
//     * 执行SPARQL Select联邦查询
//     * @param sparql
//     * @param endpoints SPARQL端点list，英文逗号隔开。例如：http://endpoint1,http://endpoint2
//     * @return Model
//     * @author 陈锟
//     * @date 2023年3月27日10:59:39
//     */
//    public static ResultSet sparqlSelectWithEndpointsNew(String sparql, String endpoints) {
//        // 调用‘comunica-query-sparql’执行SPARQL联邦查询
//        // 将返回结果封装
//        // TODO 高帅 改为调用联邦查询引擎，请移除下面的2行代码，并放开上面的注释
//        Date date1 = new Date();
//        ResultSet resultSet = null;
//        String points = endpoints.replaceAll(",", " ");
//        String cmd = "docker exec comunica comunica-sparql " + points + " \" " + sparql + " \" -t 'application/sparql-results+json' ";
//        ExecuteShellUtil instance = ExecuteShellUtil.getInstance();
//        try {
//            instance.init(IP, PORT, USERNAME, PASSWORD);
//        } catch (JSchException e) {
//            log.info("连接失败");
//            e.printStackTrace();
//        }
//        String result = "";
//        try {
//            System.out.println("执行联邦查询：\n```\n" + cmd + "\n```"); // 陈锟本机测试用
//            result = instance.execCmd(cmd);
//            InputStream targetStream = IOUtils.toInputStream(result, StandardCharsets.UTF_8.name());
//            resultSet = ResultSetFactory.fromJSON(targetStream);
//            Date date2 = new Date();
//            System.out.println("联邦查询执行完成，用时：" + (date2.getTime() - date1.getTime()) + "毫秒"); // 陈锟本机测试用
//            return resultSet;
//        } catch (Exception e) {
//            System.out.println("联邦查询执行失败"); // 陈锟本机测试用
//            log.info("error info");
//            e.printStackTrace();
//            return null;
//        } finally {
//            ExecuteShellUtil.closeConnect();
//        }
//    }

    /**
     * SPARQL查询Select；不指定端点；
     * 直接查询Jena内存Model，请在SPARQL语句中自行指定端点地址
     *
     * @param sparql
     * @return ResultSet
     * @author 陈锟
     * @date 2023年3月7日17:42:16
     */
    public static ResultSet sparqlSelect(String sparql) {
        ResultSet resultSet = null;
        QueryExecution qe = null;
        try {
            Model model = ModelFactory.createDefaultModel();
            qe = QueryExecutionFactory.create(sparql, model);
            resultSet = ResultSetFactory.copyResults(qe.execSelect());
        } catch (Throwable e) {
            log.error("执行异常错误的SPARQL语句：" + sparql);
            e.printStackTrace();
        } finally {
            if (null != qe) {
                qe.close();
            }
        }
        return resultSet;
    }

    /**
     * SPARQL查询Construct；不指定端点；
     * 直接查询Jena内存Model，请在SPARQL语句中自行指定端点地址
     *
     * @param sparql
     * @return Model
     * @author 陈锟
     * @date 2023年3月29日16:16:55
     */
    public static Model sparqlConstruct(String sparql) {
        QueryExecution qe = null;
        try {
            qe = QueryExecutionFactory.create(sparql, ModelFactory.createDefaultModel());
            Model model = qe.execConstruct();
            return model;
        } catch (Throwable e) {
            log.error("执行异常错误的SPARQL语句：" + sparql);
            e.printStackTrace();
        } finally {
            if (null != qe) {
                qe.close();
            }
        }
        return null;
    }

    /**
     * SPARQL查询count值；不指定端点；
     * 直接查询Jena内存Model，请在SPARQL语句中自行指定端点地址
     * 注意SELECT后的变量名必须为‘?count’，否则无法识别
     *
     * @param sparql
     * @return long
     * @author 陈锟
     * @date 2023年3月9日14:43:33
     */
    public static long sparqlCount(String sparql) {
        ResultSet resultSet = RdfUtils.sparqlSelect(sparql);
        if(resultSet.hasNext()){
            return resultSet.nextSolution().getLiteral("count").getLong();
        }
        return 0;
    }

    /**
     * 从iri中获取后缀
     * 实现思路：截取iri中最后一个#或/后面的字符串；如果IRI以#或/结尾，则先去掉
     *
     * @return String
     * @author 陈锟
     * @date 2023年3月8日17:19:47
     */
    public static String getIriSuffix(String iri) {
        String suffix = iri; // 如果iri不合法，直接展示原值
        if (iri.endsWith("#") || iri.endsWith("/")) {
            // 如果IRI以#或/结尾，则先去掉。例如geonames中的IRI为‘http://sws.geonames.org/1814991/’
            iri = iri.substring(0, iri.length() - 1);
        }
        if (RdfUtils.isIRI(iri)) {
            if (iri.contains("#")) {
                suffix = iri.substring(iri.lastIndexOf("#") + 1);
            } else {
                suffix = iri.substring(iri.lastIndexOf("/") + 1);
            }
        }
        return suffix;
    }

    /**
     * 判断一个字符串是否是iri
     *
     * @return str
     * @author 陈锟
     * @date 2023年3月9日16:45:02
     */
    public static boolean isIRI(String str) {
        if (StringUtils.isNotBlank(str) && (StringUtils.startsWith(str, "http://") || StringUtils.startsWith(str, "https://"))) {
            return true;
        }
        return false;
    }

}
